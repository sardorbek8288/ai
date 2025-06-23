package com.company;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

public class MyBot extends TelegramLongPollingBot {

    private final String BOT_TOKEN = "7788813812:AAHJq7eVic04t8Ds3R7L5Gh-MDqlUlJEM5c";
    private final String BOT_USERNAME = "@java_newbot";

    // Mahsulotlar va foydalanuvchi ma'lumotlari
    private Map<String, Product> products = new HashMap<>();
    private Map<Long, UserCart> userCarts = new HashMap<>();
    private Map<Long, String> userStates = new HashMap<>();

    // Admin ID
    private final Long ADMIN_ID = 123456789L; // Bu yerga admin ID ni kiriting

    public MyBot() {
        initializeProducts();
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleTextMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    private void handleTextMessage(Update update) {
        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        switch (messageText) {
            case "/start":
                sendWelcomeMessage(chatId);
                break;
            case "🛍️ Mahsulotlar":
                showCategories(chatId);
                break;
            case "🛒 Savat":
                showCart(chatId);
                break;
            case "📞 Aloqa":
                showContact(chatId);
                break;
            case "ℹ️ Ma'lumot":
                showInfo(chatId);
                break;
            case "🔧 Admin Panel":
                if (chatId.equals(ADMIN_ID)) {
                    showAdminPanel(chatId);
                } else {
                    sendMessage(chatId, "❌ Sizda admin huquqi yo'q!");
                }
                break;
            default:
                if (userStates.containsKey(chatId)) {
                    handleUserInput(chatId, messageText);
                } else {
                    sendMessage(chatId, "Noma'lum buyruq. /start buyrug'ini bosing.");
                }
        }
    }

    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (callbackData.startsWith("category_")) {
            String category = callbackData.substring(9);
            showProductsByCategory(chatId, category);
        } else if (callbackData.startsWith("product_")) {
            String productId = callbackData.substring(8);
            showProductDetails(chatId, productId);
        } else if (callbackData.startsWith("add_to_cart_")) {
            String productId = callbackData.substring(12);
            addToCart(chatId, productId);
        } else if (callbackData.startsWith("remove_from_cart_")) {
            String productId = callbackData.substring(17);
            removeFromCart(chatId, productId);
        } else if (callbackData.equals("clear_cart")) {
            clearCart(chatId);
        } else if (callbackData.equals("checkout")) {
            checkout(chatId);
        } else if (callbackData.equals("back_to_categories")) {
            showCategories(chatId);
        } else if (callbackData.equals("back_to_main")) {
            sendWelcomeMessage(chatId);
        }
    }

    private void sendWelcomeMessage(Long chatId) {
        String welcomeText = "🌟 Online Market botiga xush kelibsiz!\n\n" +
                "Bu yerda siz turli xil mahsulotlarni topishingiz va buyurtma berishingiz mumkin.\n\n" +
                "Quyidagi tugmalardan birini tanlang:";

        ReplyKeyboardMarkup keyboard = createMainKeyboard();
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(welcomeText);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private ReplyKeyboardMarkup createMainKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🛍️ Mahsulotlar"));
        row1.add(new KeyboardButton("🛒 Savat"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("📞 Aloqa"));
        row2.add(new KeyboardButton("ℹ️ Ma'lumot"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("🔧 Admin Panel"));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private void showCategories(Long chatId) {
        String text = "📂 Kategoriyalardan birini tanlang:";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        String[] categories = {"Elektronika", "Kiyim", "Kitoblar", "Sport", "Uy jihozlari"};

        for (String category : categories) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(getCategoryIcon(category) + " " + category);
            button.setCallbackData("category_" + category);
            row.add(button);
            rows.add(row);
        }

        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("🏠 Bosh menu");
        backButton.setCallbackData("back_to_main");
        backRow.add(backButton);
        rows.add(backRow);

        keyboard.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void showProductsByCategory(Long chatId, String category) {
        List<Product> categoryProducts = new ArrayList<>();
        for (Product product : products.values()) {
            if (product.getCategory().equals(category)) {
                categoryProducts.add(product);
            }
        }

        if (categoryProducts.isEmpty()) {
            sendMessage(chatId, "❌ Bu kategoriyada mahsulotlar mavjud emas.");
            return;
        }

        String text = "🛍️ " + category + " kategoriyasidagi mahsulotlar:";

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (Product product : categoryProducts) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(product.getName() + " - " + product.getPrice() + " so'm");
            button.setCallbackData("product_" + product.getId());
            row.add(button);
            rows.add(row);
        }

        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Kategoriyalar");
        backButton.setCallbackData("back_to_categories");
        backRow.add(backButton);
        rows.add(backRow);

        keyboard.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void showProductDetails(Long chatId, String productId) {
        Product product = products.get(productId);
        if (product == null) {
            sendMessage(chatId, "❌ Mahsulot topilmadi.");
            return;
        }

        String text = "📦 " + product.getName() + "\n\n" +
                "💰 Narxi: " + product.getPrice() + " so'm\n" +
                "📝 Tavsif: " + product.getDescription() + "\n" +
                "📊 Mavjud: " + (product.getStock() > 0 ? "✅ Ha (" + product.getStock() + " ta)" : "❌ Yo'q");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (product.getStock() > 0) {
            List<InlineKeyboardButton> addRow = new ArrayList<>();
            InlineKeyboardButton addButton = new InlineKeyboardButton();
            addButton.setText("🛒 Savatga qo'shish");
            addButton.setCallbackData("add_to_cart_" + productId);
            addRow.add(addButton);
            rows.add(addRow);
        }

        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("⬅️ Orqaga");
        backButton.setCallbackData("category_" + product.getCategory());
        backRow.add(backButton);
        rows.add(backRow);

        keyboard.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void addToCart(Long chatId, String productId) {
        Product product = products.get(productId);
        if (product == null || product.getStock() <= 0) {
            sendMessage(chatId, "❌ Mahsulotni savatga qo'shib bo'lmaydi.");
            return;
        }

        UserCart cart = userCarts.computeIfAbsent(chatId, k -> new UserCart());
        cart.addProduct(product);

        sendMessage(chatId, "✅ " + product.getName() + " savatga qo'shildi!");
    }

    private void removeFromCart(Long chatId, String productId) {
        UserCart cart = userCarts.get(chatId);
        if (cart != null) {
            Product product = products.get(productId);
            if (product != null) {
                cart.removeProduct(productId);
                sendMessage(chatId, "✅ " + product.getName() + " savatdan o'chirildi!");
            }
        }
    }

    private void showCart(Long chatId) {
        UserCart cart = userCarts.get(chatId);

        if (cart == null || cart.getItems().isEmpty()) {
            sendMessage(chatId, "🛒 Savatingiz bo'sh.");
            return;
        }

        StringBuilder text = new StringBuilder("🛒 Sizning savatingiz:\n\n");
        double total = 0;

        for (Map.Entry<String, Integer> entry : cart.getItems().entrySet()) {
            Product product = products.get(entry.getKey());
            if (product != null) {
                int quantity = entry.getValue();
                double itemTotal = product.getPrice() * quantity;
                total += itemTotal;

                text.append("• ").append(product.getName())
                        .append(" x").append(quantity)
                        .append(" = ").append(itemTotal).append(" so'm\n");
            }
        }

        text.append("\n💰 Jami: ").append(total).append(" so'm");

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Mahsulotlarni o'chirish tugmalari
        for (String productId : cart.getItems().keySet()) {
            Product product = products.get(productId);
            if (product != null) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton removeButton = new InlineKeyboardButton();
                removeButton.setText("❌ " + product.getName() + " o'chirish");
                removeButton.setCallbackData("remove_from_cart_" + productId);
                row.add(removeButton);
                rows.add(row);
            }
        }

        // Amallar
        List<InlineKeyboardButton> actionRow1 = new ArrayList<>();
        InlineKeyboardButton checkoutButton = new InlineKeyboardButton();
        checkoutButton.setText("💳 Buyurtma berish");
        checkoutButton.setCallbackData("checkout");
        actionRow1.add(checkoutButton);
        rows.add(actionRow1);

        List<InlineKeyboardButton> actionRow2 = new ArrayList<>();
        InlineKeyboardButton clearButton = new InlineKeyboardButton();
        clearButton.setText("🗑️ Savatni tozalash");
        clearButton.setCallbackData("clear_cart");
        actionRow2.add(clearButton);
        rows.add(actionRow2);

        List<InlineKeyboardButton> backRow = new ArrayList<>();
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("🏠 Bosh menu");
        backButton.setCallbackData("back_to_main");
        backRow.add(backButton);
        rows.add(backRow);

        keyboard.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text.toString());
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void clearCart(Long chatId) {
        userCarts.remove(chatId);
        sendMessage(chatId, "✅ Savat tozalandi!");
    }

    private void checkout(Long chatId) {
        UserCart cart = userCarts.get(chatId);
        if (cart == null || cart.getItems().isEmpty()) {
            sendMessage(chatId, "❌ Savatingiz bo'sh!");
            return;
        }

        StringBuilder orderText = new StringBuilder("📋 Sizning buyurtmangiz:\n\n");
        double total = 0;

        for (Map.Entry<String, Integer> entry : cart.getItems().entrySet()) {
            Product product = products.get(entry.getKey());
            if (product != null) {
                int quantity = entry.getValue();
                double itemTotal = product.getPrice() * quantity;
                total += itemTotal;

                orderText.append("• ").append(product.getName())
                        .append(" x").append(quantity)
                        .append(" = ").append(itemTotal).append(" so'm\n");
            }
        }

        orderText.append("\n💰 Jami: ").append(total).append(" so'm\n\n");
        orderText.append("📞 Buyurtmani tasdiqlash uchun bizga qo'ng'iroq qiling yoki xabar yuboring:\n");
        orderText.append("📱 Telefon: +998 90 123 45 67\n");
        orderText.append("📧 Email: info@onlinemarket.uz\n\n");
        orderText.append("🚚 Yetkazib berish 1-2 ish kuni ichida amalga oshiriladi.");

        // Admin ga xabar yuborish
        if (ADMIN_ID != null) {
            String adminMessage = "🆕 Yangi buyurtma!\n\n" +
                    "👤 Mijoz: @" + chatId + "\n" +
                    orderText.toString();
            sendMessage(ADMIN_ID, adminMessage);
        }

        sendMessage(chatId, orderText.toString());

        // Savatni tozalash
        userCarts.remove(chatId);
    }

    private void showContact(Long chatId) {
        String contactText = "📞 Biz bilan bog'lanish:\n\n" +
                "📱 Telefon: +998 90 123 45 67\n" +
                "📧 Email: info@onlinemarket.uz\n" +
                "🌐 Website: www.onlinemarket.uz\n" +
                "📍 Manzil: Toshkent sh., Amir Temur ko'chasi 1-uy\n\n" +
                "⏰ Ish vaqti:\n" +
                "Dushanba-Juma: 9:00-18:00\n" +
                "Shanba: 10:00-16:00\n" +
                "Yakshanba: Dam olish kuni";

        sendMessage(chatId, contactText);
    }

    private void showInfo(Long chatId) {
        String infoText = "ℹ️ Online Market haqida:\n\n" +
                "🛍️ Bizning do'konimizda siz turli xil mahsulotlarni topishingiz mumkin:\n" +
                "• Elektronika\n" +
                "• Kiyim-kechak\n" +
                "• Kitoblar\n" +
                "• Sport anjomlari\n" +
                "• Uy jihozlari\n\n" +
                "✅ Bizning afzalliklarimiz:\n" +
                "• Sifatli mahsulotlar\n" +
                "• Tez yetkazib berish\n" +
                "• Qulay narxlar\n" +
                "• 24/7 qo'llab-quvvatlash\n\n" +
                "🚚 Yetkazib berish bepul (50,000 so'mdan yuqori buyurtmalar uchun)";

        sendMessage(chatId, infoText);
    }

    private void showAdminPanel(Long chatId) {
        String adminText = "🔧 Admin Panel\n\n" +
                "Mavjud buyruqlar:\n" +
                "/stats - Statistika ko'rish\n" +
                "/addproduct - Yangi mahsulot qo'shish\n" +
                "/removeproduct - Mahsulot o'chirish\n" +
                "/updatestock - Stok yangilash";

        sendMessage(chatId, adminText);
    }

    private void handleUserInput(Long chatId, String input) {
        String state = userStates.get(chatId);
        // Bu yerda foydalanuvchi kiritgan ma'lumotlarni qayta ishlash logikasi
        userStates.remove(chatId);
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String getCategoryIcon(String category) {
        switch (category) {
            case "Elektronika": return "📱";
            case "Kiyim": return "👕";
            case "Kitoblar": return "📚";
            case "Sport": return "⚽";
            case "Uy jihozlari": return "🏠";
            default: return "📦";
        }
    }

    private void initializeProducts() {
        // Elektronika
        products.put("1", new Product("1", "iPhone 14", "Elektronika", 12000000, "Apple iPhone 14 128GB", 10));
        products.put("2", new Product("2", "Samsung Galaxy S23", "Elektronika", 10000000, "Samsung Galaxy S23 256GB", 15));
        products.put("3", new Product("3", "Laptop Dell", "Elektronika", 8000000, "Dell Inspiron 15 3000", 5));

        // Kiyim
        products.put("4", new Product("4", "Ko'ylak", "Kiyim", 250000, "Erkaklar uchun klassik ko'ylak", 20));
        products.put("5", new Product("5", "Jeans", "Kiyim", 300000, "Ayollar uchun jeans shim", 25));

        // Kitoblar
        products.put("6", new Product("6", "O'zbek adabiyoti", "Kitoblar", 50000, "Klassik o'zbek adabiyoti", 30));
        products.put("7", new Product("7", "Dasturlash kitobi", "Kitoblar", 120000, "Java dasturlash kitobi", 12));

        // Sport
        products.put("8", new Product("8", "Futbol to'pi", "Sport", 150000, "Professional futbol to'pi", 8));
        products.put("9", new Product("9", "Yoga kilimi", "Sport", 80000, "Yoga va fitnes uchun kilim", 18));

        // Uy jihozlari
        products.put("10", new Product("10", "Blender", "Uy jihozlari", 500000, "Kuchli blender 1000W", 6));
        products.put("11", new Product("11", "Mikroto'lqinli pech", "Uy jihozlari", 1200000, "Samsung mikroto'lqinli pech", 4));
    }

    // Product sinfi
    class Product {
        private String id;
        private String name;
        private String category;
        private double price;
        private String description;
        private int stock;

        public Product(String id, String name, String category, double price, String description, int stock) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.price = price;
            this.description = description;
            this.stock = stock;
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public double getPrice() { return price; }
        public String getDescription() { return description; }
        public int getStock() { return stock; }

        // Setters
        public void setStock(int stock) { this.stock = stock; }
        public void setPrice(double price) { this.price = price; }
    }

    // UserCart sinfi
    class UserCart {
        private Map<String, Integer> items = new HashMap<>();

        public void addProduct(Product product) {
            items.put(product.getId(), items.getOrDefault(product.getId(), 0) + 1);
        }

        public void removeProduct(String productId) {
            items.remove(productId);
        }

        public Map<String, Integer> getItems() {
            return items;
        }

        public void clear() {
            items.clear();
        }
    }
}