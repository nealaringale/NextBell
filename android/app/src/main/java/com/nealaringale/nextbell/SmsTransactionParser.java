package com.nealaringale.nextbell;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses a narrow set of bank transaction alerts without retaining the message text. */
public final class SmsTransactionParser {
    private static final Pattern AMOUNT = Pattern.compile(
            "(?i)(?:₹|\\bINR\\b|\\bRs\\.?\\s*)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)"
    );
    private static final Pattern BALANCE = Pattern.compile(
            "(?i)(?:available|avl\\.?|closing|current)\\s+(?:balance|bal\\.?)"
                    + "(?:\\s+(?:is|of))?\\s*[:=\\-]?\\s*(?:₹|INR|Rs\\.?\\s*)?"
                    + "\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)"
    );
    private static final Pattern BARE_TRANSACTION_AMOUNT = Pattern.compile(
            "(?i)\\b(?:debited|credited|deducted|withdrawn)\\s+(?:by|of)\\s+"
                    + "([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\b"
    );
    private static final Pattern EXPLICIT_DATE = Pattern.compile(
            "(?i)(?:on|dated|txn(?:\\s+date)?)\\s*[:\\-]?\\s*"
                    + "(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})"
    );
    private static final Pattern REFUND = Pattern.compile(
            "(?i)refund|reversal|reversed|cashback|credited|credit to|received|deposit|paid back"
    );
    private static final Pattern DEBIT_TERMS = Pattern.compile(
            "(?i)debited|deducted|withdrawn|withdrawal|spent|purchase|payment|paid|sent|transfer to"
    );
    private static final Pattern RAIL_SUCCESS = Pattern.compile(
            "(?i)(?:upi|atm|pos|neft|imps|rtgs|card|nach|ach).{0,90}"
                    + "(?:successful|successfully|completed|processed|paid)"
    );
    private static final Pattern FAILED = Pattern.compile(
            "(?i)\\b(?:failed|failure|declined|unsuccessful|rejected|pending|not processed)\\b"
    );
    private static final Pattern MERCHANT = Pattern.compile(
            "(?i)\\b(?:at|to|merchant|towards)\\s+(.{2,56}?)"
                    + "(?=\\s+(?:on|ref|upi|txn|transaction|avl|available|balance|"
                    + "a/c|ac|from|using|via)\\b|[,.;]|$)"
    );

    public static final String DEBIT = "DEBIT";
    public static final String CREDIT = "CREDIT";

    public static final class ParsedTransaction {
        public final long amountPaise;
        public final long availableBalancePaise;
        public final LocalDate date;
        public final String direction;
        public final String merchant;
        public final String category;
        public final String paymentMode;
        public final float confidence;
        public final String fingerprint;

        ParsedTransaction(long amountPaise, long availableBalancePaise, LocalDate date,
                          String direction, String merchant, String category,
                          String paymentMode, float confidence, String fingerprint) {
            this.amountPaise = amountPaise;
            this.availableBalancePaise = availableBalancePaise;
            this.date = date;
            this.direction = direction;
            this.merchant = merchant;
            this.category = category;
            this.paymentMode = paymentMode;
            this.confidence = confidence;
            this.fingerprint = fingerprint;
        }
    }

    private SmsTransactionParser() {}

    public static ParsedTransaction parse(String sender, String body, LocalDate receivedDate) {
        if (!isBankLikeSender(sender) || body == null || body.trim().isEmpty()
                || receivedDate == null) return null;
        String normalized = body.replace('\u00a0', ' ').replaceAll("\\s+", " ").trim();
        if (FAILED.matcher(normalized).find() || normalized.matches("(?i).*\\bnot\\s+debited\\b.*")) {
            return null;
        }

        Matcher amountMatcher = AMOUNT.matcher(normalized);
        boolean hasCurrencyAmount = amountMatcher.find();
        Matcher bareAmountMatcher = BARE_TRANSACTION_AMOUNT.matcher(normalized);
        boolean hasBareAmount = bareAmountMatcher.find();
        if (!hasCurrencyAmount && !hasBareAmount) return null;
        String amountText = hasBareAmount
                && (!hasCurrencyAmount || bareAmountMatcher.start() < amountMatcher.start())
                ? bareAmountMatcher.group(1) : amountMatcher.group(1);

        boolean isCredit = REFUND.matcher(normalized).find();
        boolean isDebit = DEBIT_TERMS.matcher(normalized).find()
                || RAIL_SUCCESS.matcher(normalized).find();
        if (!isCredit && !isDebit) return null;

        long amountPaise;
        try {
            amountPaise = toPaise(amountText);
        } catch (RuntimeException invalidAmount) {
            return null;
        }
        if (amountPaise <= 0L) return null;

        long balancePaise = -1L;
        Matcher balanceMatcher = BALANCE.matcher(normalized);
        if (balanceMatcher.find()) {
            try {
                balancePaise = toPaise(balanceMatcher.group(1));
            } catch (RuntimeException ignored) {
                balancePaise = -1L;
            }
        }

        String direction = isCredit ? CREDIT : DEBIT;
        String paymentMode = paymentMode(normalized);
        String merchant = MerchantCatalog.match(normalized);
        if (merchant.isEmpty()) merchant = extractMerchant(normalized);
        String category = MerchantCatalog.categoryFor(normalized, merchant, direction);
        String fingerprint = fingerprint(sender, normalized);
        float confidence = merchant.isEmpty() ? 0.72f : 0.92f;

        return new ParsedTransaction(amountPaise, balancePaise,
                transactionDate(normalized, receivedDate), direction, merchant,
                category, paymentMode, confidence, fingerprint);
    }

    private static boolean isBankLikeSender(String sender) {
        if (sender == null) return false;
        String value = sender.trim().toUpperCase(Locale.ENGLISH);
        if (value.length() < 4 || value.length() > 18 || value.contains(" ")) return false;
        if (value.matches("[A-Z]{2,3}-[A-Z0-9]{4,12}")) return true;
        return value.matches("(?:HDFC|HDFCBK|SBI|SBIINB|SBIUPI|ICICI|ICICIB|AXIS|AXISBK|"
                + "KOTAK|KOTAKB|YESBNK|YESBANK|PNBSMS|BOIIND|BARODA|CANBNK|INDUSB|"
                + "IDFCFB|PAYTM|PHONEPE|BANKEX)[A-Z0-9]{0,6}");
    }

    private static LocalDate transactionDate(String body, LocalDate fallback) {
        Matcher matcher = EXPLICIT_DATE.matcher(body);
        if (!matcher.find()) return fallback;
        try {
            int day = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int year = Integer.parseInt(matcher.group(3));
            if (year < 100) year += 2000;
            return LocalDate.of(year, month, day);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static long toPaise(String value) {
        return new BigDecimal(value.replace(",", ""))
                .movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private static String paymentMode(String body) {
        String lower = body.toLowerCase(Locale.ENGLISH);
        if (lower.contains("upi")) return "UPI";
        if (lower.contains("atm")) return "ATM";
        if (lower.contains("pos") || lower.contains("card")) return "Card";
        if (lower.contains("neft")) return "NEFT";
        if (lower.contains("imps")) return "IMPS";
        if (lower.contains("rtgs")) return "RTGS";
        if (lower.contains("nach") || lower.contains("ach")) return "Bank Transfer";
        return "Bank Transfer";
    }

    private static String extractMerchant(String body) {
        Matcher matcher = MERCHANT.matcher(body);
        if (!matcher.find()) return "";
        String value = matcher.group(1).replaceAll("(?i)\\b(?:a/c|ac|account)\\b.*$", "")
                .replaceAll("[.\\s]+$", "").trim();
        if (value.length() > 40 || value.matches("(?i)your account|the account|account")) return "";
        return value;
    }

    private static String fingerprint(String sender, String normalizedBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = (sender == null ? "" : sender.trim().toUpperCase(Locale.ENGLISH))
                    + "|" + normalizedBody.toLowerCase(Locale.ENGLISH);
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) out.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            return out.toString();
        } catch (Exception impossible) {
            return "";
        }
    }

    /** Small, local, human-maintained seed catalog; corrections extend it per user. */
    private static final class MerchantCatalog {
        private static final String[][] MERCHANTS = {
                {"Swiggy", "Food & Drinks", "swiggy"},
                {"Zomato", "Food & Drinks", "zomato"},
                {"Blinkit", "Groceries", "blinkit"},
                {"Zepto", "Groceries", "zepto"},
                {"BigBasket", "Groceries", "bigbasket, big basket"},
                {"DMart", "Groceries", "dmart, d mart, avenue supermarts"},
                {"JioMart", "Groceries", "jiomart, jio mart"},
                {"Reliance Smart", "Groceries", "reliance smart, smart bazaar"},
                {"Amazon", "Shopping", "amazon, amzn"},
                {"Flipkart", "Shopping", "flipkart, flip kart"},
                {"Myntra", "Shopping", "myntra"},
                {"Ajio", "Shopping", "ajio"},
                {"Meesho", "Shopping", "meesho"},
                {"Uber", "Travel", "uber"},
                {"Ola", "Travel", "ola cabs, ola"},
                {"Rapido", "Travel", "rapido"},
                {"IRCTC", "Travel", "irctc, indian railways"},
                {"redBus", "Travel", "redbus, red bus"},
                {"MakeMyTrip", "Travel", "makemytrip, make my trip, mmt"},
                {"IndianOil", "Travel", "indianoil, indian oil, iocl"},
                {"Bharat Petroleum", "Travel", "bharat petroleum, bpcl"},
                {"Hindustan Petroleum", "Travel", "hindustan petroleum, hpcl"},
                {"Shell", "Travel", "shell petrol, shell"},
                {"Netflix", "Subscriptions", "netflix"},
                {"Spotify", "Subscriptions", "spotify"},
                {"YouTube Premium", "Subscriptions", "youtube premium, youtube"},
                {"Prime Video", "Subscriptions", "prime video, amazon prime"},
                {"Jio", "Bills & Recharge", "jio recharge, reliance jio"},
                {"Airtel", "Bills & Recharge", "airtel recharge, bharti airtel"},
                {"Vi", "Bills & Recharge", "vodafone idea, vi recharge"},
                {"BSNL", "Bills & Recharge", "bsnl"},
                {"Google Play", "Entertainment", "google play, play store"},
                {"BookMyShow", "Fun & Social", "bookmyshow, book my show"},
                {"PVR INOX", "Fun & Social", "pvr, inox"},
                {"Apollo Pharmacy", "Health", "apollo pharmacy, apollo 24|7"},
                {"PharmEasy", "Health", "pharmeasy, pharm easy"},
                {"Tata 1mg", "Health", "1mg, tata 1mg"},
                {"Decathlon", "Shopping", "decathlon"},
                {"IKEA", "Shopping", "ikea"},
                {"Croma", "Shopping", "croma"},
                {"Reliance Digital", "Shopping", "reliance digital"},
                {"Maharashtra State Electricity", "Home & Bills", "mahadiscom, mseb, mahavitaran"},
                {"Tata Power", "Home & Bills", "tata power"},
                {"Mahanagar Gas", "Home & Bills", "mahanagar gas, mgl"},
                {"Tata Play", "Home & Bills", "tata play, tata sky"},
                {"College Fees", "College", "college fee, tuition fee, examination fee, exam fee"},
                {"Canteen", "Food & Drinks", "canteen, cafeteria, mess"},
                {"Cafe", "Food & Drinks", "cafe, coffee shop, tea stall"},
                {"Grocery Store", "Groceries", "grocery, supermarket, kirana"},
                {"Rent", "Rent & Accommodation", "rent, hostel fee, accommodation"}
        };

        static String match(String body) {
            String normalized = normalize(body);
            for (String[] merchant : MERCHANTS) {
                for (String alias : merchant[2].split(",")) {
                    String candidate = normalize(alias);
                    if (candidate.isEmpty()) continue;
                    if (containsPhrase(normalized, candidate)) return merchant[0];
                    if (!candidate.contains(" ") && candidate.length() >= 5
                            && fuzzyWordPresent(normalized, candidate)) return merchant[0];
                }
            }
            return "";
        }

        static String categoryFor(String body, String merchant, String direction) {
            if (direction.equals(CREDIT)) return "Income & Refunds";
            String normalized = normalize(body + " " + merchant);
            String known = match(body);
            if (!known.isEmpty()) {
                for (String[] item : MERCHANTS) if (item[0].equals(known)) return item[1];
            }
            if (containsAny(normalized, "tea", "coffee", "snack", "chai", "bakery")) {
                return "Tea & Coffee";
            }
            if (containsAny(normalized, "petrol", "fuel", "diesel")) {
                return "Transport & Fuel";
            }
            if (containsAny(normalized, "metro", "bus", "taxi", "railway", "travel")) {
                return "Travel";
            }
            if (containsAny(normalized, "rent", "hostel", "electricity", "water bill", "gas bill")) {
                return "Home & Bills";
            }
            if (containsAny(normalized, "grocery", "vegetable", "supermarket", "kirana")) {
                return "Groceries";
            }
            if (containsAny(normalized, "college", "tuition", "university", "exam fee", "stationery")) {
                return "College";
            }
            if (containsAny(normalized, "recharge", "mobile bill", "broadband", "internet bill")) {
                return "Bills & Recharge";
            }
            if (containsAny(normalized, "subscription", "monthly plan", "membership")) {
                return "Subscriptions";
            }
            if (containsAny(normalized, "atm", "cash withdrawal", "self transfer")) {
                return "Transfers & ATM";
            }
            if (containsAny(normalized, "restaurant", "food", "dining", "swiggy", "zomato")) {
                return "Food & Drinks";
            }
            return "Other";
        }

        private static boolean containsPhrase(String text, String phrase) {
            return (" " + text + " ").contains(" " + phrase + " ");
        }

        private static boolean containsAny(String text, String... terms) {
            for (String term : terms) if (containsPhrase(text, normalize(term))) return true;
            return false;
        }

        private static String normalize(String value) {
            return value.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", " ")
                    .trim().replaceAll("\\s+", " ");
        }

        private static boolean fuzzyWordPresent(String normalizedText, String candidate) {
            for (String word : normalizedText.split(" ")) {
                if (Math.abs(word.length() - candidate.length()) > 1) continue;
                if (editDistanceAtMostOne(word, candidate)) return true;
            }
            return false;
        }

        private static boolean editDistanceAtMostOne(String a, String b) {
            if (a.equals(b)) return true;
            if (Math.abs(a.length() - b.length()) > 1) return false;
            int i = 0, j = 0, edits = 0;
            while (i < a.length() && j < b.length()) {
                if (a.charAt(i) == b.charAt(j)) { i++; j++; continue; }
                if (++edits > 1) return false;
                if (a.length() > b.length()) i++;
                else if (b.length() > a.length()) j++;
                else { i++; j++; }
            }
            return edits + (i < a.length() || j < b.length() ? 1 : 0) <= 1;
        }
    }
}
