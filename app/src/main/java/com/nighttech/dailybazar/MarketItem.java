package com.nighttech.dailybazar;

/**
 * MarketItem — immutable data model for a single product in the price grid.
 *
 * Fields
 * ──────
 *  name        : display name (e.g. "Tomato")
 *  price       : formatted price string (e.g. "Rs. 120/kg")
 *  imageResId  : drawable resource ID for the product thumbnail
 *  isTrendUp   : true → green up-arrow, false → red down-arrow
 *  category    : used for future filtering (e.g. "Vegetable", "Grain")
 */
public class MarketItem {

    private final String name;
    private final String price;
    private final int    imageResId;
    private final boolean isTrendUp;
    private final String category;

    public MarketItem(String name, String price, int imageResId,
                      boolean isTrendUp, String category) {
        this.name       = name;
        this.price      = price;
        this.imageResId = imageResId;
        this.isTrendUp  = isTrendUp;
        this.category   = category;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String  getName()       { return name; }
    public String  getPrice()      { return price; }
    public int     getImageResId() { return imageResId; }
    public boolean isTrendUp()     { return isTrendUp; }
    public String  getCategory()   { return category; }
}