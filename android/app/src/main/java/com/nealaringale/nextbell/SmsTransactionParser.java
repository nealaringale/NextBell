package com.nealaringale.nextbell;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SmsTransactionParser {
    public static final class ParsedTransaction {
        public final long amountPaise;
        public final String merchant;
        public final String category;
        public final String paymentMode;
        public final float confidence;
        public final long smsTime;
        public final String fingerprint;

        ParsedTransaction(
                long amountPaise,
                String merchant,
                String category,
                String paymentMode,
                float confidence,
                long smsTime,
                String fingerprint
        ) {
            this.amountPaise = amountPaise;
            this.merchant = merchant;
            this.category = category;
            this.paymentMode = paymentMode;
            this.confidence = confidence;
            this.smsTime = smsTime;
            this.fingerprint = fingerprint;
        }
    }

    private static final Pattern CURRENCY_AMOUNT = Pattern.compile(
            "(?:₹|rs\.?|inr\.?)[ ]*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern KEYWORD_AMOUNT = Pattern.compile(
            "(?:debited|debit|spent|paid|purchase|withdrawn|txn|transaction)"
                    + "(?:[^0-9₹]{0,80})([0-9][0-9,]*(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern MERCHANT_AFTER_WORD = Pattern.compile(
            "(?:at|to|for|towards|merchant)[ :\-]+([A-Za-z0-9][A-Za-z0-9 &._'()/#@-]{2,48})",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern VPA = Pattern.compile(
            "\\b([A-Za-z0-9][A-Za-z0-9._-]{2,})@[A-Za-z][A-Za-z0-9._-]{1,}\b"
    );

    private static final String[] DEBIT_WORDS = {
            "debited", "debit", "spent", "paid", "purchase", "withdrawn",
            "sent", "payment successful", "transaction successful", "txn successful"
    };

    private static final String[] CREDIT_WORDS = {
            "credited", "credit", "received", "refund", "refunded",
            "cashback", "reversal", "reversed", "interest credited"
    };

    private static final String[] IGNORE_WORDS = {
            "otp", "one time password", "verification code", "login",
            "failed", "declined", "unsuccessful", "cancelled", "canceled",
            "will be debited", "will be charged", "standing instruction due"
    };

    private static final Map<String, String> CATEGORY_RULES = new HashMap<>();

    static {
        add("Food & Drinks", "swiggy,zomato,blinkit,zepto,bigbasket,instamart,ubereats,dominos,pizza hut,mcdonald,kfc,subway,starbucks,cafe,café,coffee,tea,chai,canteen,mess,restaurant,hotel,bakery,juice,dhaba,snacks,food,thali,ice cream,icecream,paratha,poha,samosa,vada pav");
        add("Travel", "uber,ola,rapido,redbus,irctc,metro,bus,train,parking,toll,petrol,fuel,iocl,indian oil,bpcl,bharat petroleum,hpcl,hindustan petroleum,shell,reliance petroleum,jio-bp,pump,gas station,auto,rickshaw,cab");
        add("College", "college,university,institute,school,tuition,coaching,exam,admission,fees,fee,semester,stationery,xerox,photocopy,print,printing,notebook,book,books,lab,project,assignment");
        add("Home & Bills", "rent,electricity,mseb,mahadiscom,water,gas cylinder,indane,hp gas,bharatgas,broadband,wifi,internet,airtel fiber,jiofiber,dish tv,dth");
        add("Mobile", "mobile recharge,recharge,airtel,jio,vi,vodafone,bsnl,data pack,talktime");
        add("Personal", "salon,barber,haircut,laundry,cleaning,tailor,spa,personal care");
        add("Fun & Social", "pvr,inox,cinema,movie,bookmyshow,bowling,arcade,game,concert,event,cafe social,party");
        add("Shopping", "amazon,flipkart,myntra,ajio,meesho,nykaa,decathlon,ikea,store,mart,supermarket,market,clothing,shoe,shoes,electronics");
        add("Health", "pharmacy,medical,medicine,medplus,apollo,netmeds,1mg,clinic,hospital,doctor,diagnostic,lab test");
    }

    private static void add(String category, String terms) {
        CATEGORY_RULES.put(category, terms);
    }

    private SmsTransactionParser() {}

    public static ParsedTransaction parse(String sender, String body, long smsTime) {
        if (body == null) return null;
        String text = normalize(body);
        String lower = text.toLowerCase(Locale.ENGLISH);

        if (!containsAny(lower, DEBIT_WORDS)) return null;
        if (containsAny(lower, CREDIT_WORDS) || containsAny(lower, IGNORE_WORDS)) return null;

        long amountPaise = extractAmountPaise(text);
        if (amountPaise <= 0) return null;

        String merchant = extractMerchant(text);
        String category = categorize(lower, merchant);
        String paymentMode = detectPayment(lower);
        float confidence = 0.52f;
        if (!merchant.isEmpty()) confidence += 0.20f;
        if (!"Other".equals(category)) confidence += 0.20f;
        if (!"Other".equals(paymentMode)) confidence += 0.05f;
        if (containsAny(lower, new String[]{"upi", "bank", "account", "a/c", "card"})) confidence += 0.03f;
        confidence = Math.min(0.99f, confidence);

        String fingerprint = fingerprint(sender, amountPaise, smsTime, merchant, text);
        return new ParsedTransaction(
                amountPaise,
                merchant,
                category,
                paymentMode,
                confidence,
                smsTime,
                fingerprint
        );
    }

    private static long extractAmountPaise(String text) {
        Matcher m = CURRENCY_AMOUNT.matcher(text);
        if (m.find()) return toPaise(m.group(1));

        m = KEYWORD_AMOUNT.matcher(text);
        if (m.find()) return toPaise(m.group(1));

        return 0L;
    }

    private static long toPaise(String raw) {
        try {
            return new BigDecimal(raw.replace(",", ""))
                    .movePointRight(2)
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private static String extractMerchant(String text) {
        Matcher m = MERCHANT_AFTER_WORD.matcher(text);
        if (m.find()) {
            String value = cleanMerchant(m.group(1));
            if (isNoiseMerchant(value)) return "";
            return value;
        }

        m = VPA.matcher(text);
        if (m.find()) {
            String value = cleanMerchant(m.group(1));
            if (!isNoiseMerchant(value)) return value;
        }

        return "";
    }

    private static String cleanMerchant(String raw) {
        String value = raw == null ? "" : raw.trim();
        value = value.replaceAll("\\s+", " ");
        value = value.replaceAll("(?i)\\b(on|dated|date|using|via|through|ref|reference|txn|transaction)\\b.*$", "");
        value = value.replaceAll("[,.;:]+$", "").trim();
        if (value.length() > 42) value = value.substring(0, 42).trim();
        return value;
    }

    private static boolean isNoiseMerchant(String value) {
        if (value.isEmpty()) return true;
        String lower = value.toLowerCase(Locale.ENGLISH);
        return lower.equals("your account")
                || lower.equals("your a/c")
                || lower.equals("bank")
                || lower.equals("account")
                || lower.equals("card")
                || lower.equals("mobile");
    }

    public static String categorize(String bodyLower, String merchant) {
        String haystack = (merchant + " " + bodyLower).toLowerCase(Locale.ENGLISH);
        String best = "Other";
        int bestScore = 0;

        for (Map.Entry<String, String> entry : CATEGORY_RULES.entrySet()) {
            int score = 0;
            for (String term : entry.getValue().split(",")) {
                if (haystack.contains(term)) score += term.length() >= 5 ? 3 : 2;
            }
            if (score > bestScore) {
                bestScore = score;
                best = entry.getKey();
            }
        }

        return best;
    }

    private static String detectPayment(String lower) {
        if (containsAny(lower, new String[]{"upi", "vpa", "upi ref", "upi id"})) return "UPI";
        if (containsAny(lower, new String[]{"credit card", "credit card ending", "cc ending"})) return "Card";
        if (containsAny(lower, new String[]{"debit card", "debit card ending", "dc ending"})) return "Card";
        if (containsAny(lower, new String[]{"atm", "cash withdrawal", "cash withdrawn"})) return "Cash";
        if (containsAny(lower, new String[]{"imps", "neft", "rtgs", "bank transfer"})) return "Bank Transfer";
        return "Other";
    }

    private static String paymentModeKey(String normalizedText) {
        String lower = normalizedText.toLowerCase(Locale.ENGLISH);
        if (containsAny(lower, new String[]{"upi", "vpa"})) return "upi";
        if (containsAny(lower, new String[]{"card", "atm"})) return "card";
        if (containsAny(lower, new String[]{"imps", "neft", "rtgs"})) return "bank";
        return "other";
    }

    private static boolean containsAny(String value, String[] words) {
        for (String word : words) {
            if (value.contains(word)) return true;
        }
        return false;
    }

    private static String normalize(String text) {
        return text.replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String fingerprint(String sender, long amountPaise, long smsTime,
                                      String merchant, String normalizedText) {
        try {
            LocalDate date = LocalDate.ofInstant(
                    java.time.Instant.ofEpochMilli(smsTime),
                    ZoneId.of("Asia/Kolkata")
            );
            long minuteBucket = smsTime / 60_000L;
            String input = amountPaise + "|" + date + "|" + minuteBucket
                    + "|" + merchant.toLowerCase(Locale.ENGLISH)
                    + "|" + paymentModeKey(normalizedText);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : hash) out.append(String.format(Locale.ENGLISH, "%02x", b));
            return out.toString();
        } catch (Exception ignored) {
            return String.valueOf(sender) + "|" + amountPaise + "|" + smsTime;
        }
    }
}
