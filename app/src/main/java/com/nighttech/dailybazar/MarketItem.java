package com.nighttech.dailybazar;

/**
 * MarketItem — data model for a market price product.
 * imageUrl is a Firebase Storage URL; imageResId is fallback for local data.
 */
public class MarketItem {

    private final String name;
    private final String price;
    private final int    imageResId;
    private final boolean isTrendUp;
    private final String category;
    private String imageUrl; // Firebase Storage URL (nullable)

    // Constructor for local/dummy data
    public MarketItem(String name, String price, int imageResId,
                      boolean isTrendUp, String category) {
        this.name       = name;
        this.price      = price;
        this.imageResId = imageResId;
        this.isTrendUp  = isTrendUp;
        this.category   = category;
        this.imageUrl   = null;
    }

    // Constructor for Firebase data
    public MarketItem(String name, String price, String imageUrl,
                      boolean isTrendUp, String category) {
        this.name       = name;
        this.price      = price;
        this.imageResId = 0;
        this.isTrendUp  = isTrendUp;
        this.category   = category;
        this.imageUrl   = imageUrl;
    }

    // Required no-arg constructor for Firestore deserialization
    public MarketItem() {
        this.name = ""; this.price = ""; this.imageResId = 0;
        this.isTrendUp = false; this.category = "";
    }

    public String  getName()       { return name; }
    public String  getPrice()      { return price; }
    public int     getImageResId() { return imageResId; }
    public boolean isTrendUp()     { return isTrendUp; }
    public String  getCategory()   { return category; }
    public String  getImageUrl()   { return imageUrl; }
    public void    setImageUrl(String url) { this.imageUrl = url; }
}