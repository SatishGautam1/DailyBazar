package com.nighttech.dailybazar;

import com.google.firebase.database.PropertyName;


/**
 * LivePriceItem — data model for a single live-price entry read from
 * Firebase Realtime Database.
 *
 * Expected RTDB node shape (under /livePrices/{category}/{itemId}):
 * {
 *   "name":      "Gold (24k)",
 *   "price":     "Rs. 1,45,000/tola",
 *   "imageUrl":  "https://res.cloudinary.com/dwwz8f5jd/...",   // optional
 *   "isTrendUp": true,
 *   "category":  "Gold",
 *   "unit":      "tola"   // optional display hint
 * }
 *
 * The no-arg constructor is required by the Firebase SDK for automatic
 * DataSnapshot.getValue(LivePriceItem.class) deserialization.
 */
public class LivePriceItem {

    private String name;
    private String price;
    private String imageUrl;
    @PropertyName("isTrendUp")
    private boolean isTrendUp;
    private String category;
    private String unit;

    // Required by Firebase RTDB deserializer
    public LivePriceItem() {}

    public LivePriceItem(String name, String price, String imageUrl,
                         boolean isTrendUp, String category, String unit) {
        this.name      = name;
        this.price     = price;
        this.imageUrl  = imageUrl;
        this.isTrendUp = isTrendUp;
        this.category  = category;
        this.unit      = unit;
    }

    public String  getName()      { return name != null ? name : ""; }
    public String  getPrice()     { return price != null ? price : ""; }
    public String  getImageUrl()  { return imageUrl != null ? imageUrl : ""; }
    public String  getCategory()  { return category != null ? category : ""; }
    public String  getUnit()      { return unit != null ? unit : ""; }

    public void setName(String name)          { this.name = name; }
    public void setPrice(String price)        { this.price = price; }
    public void setImageUrl(String imageUrl)  { this.imageUrl = imageUrl; }
    public void setCategory(String category)  { this.category = category; }
    public void setUnit(String unit)          { this.unit = unit; }

    // Replace the isTrendUp field and its accessors:
    @PropertyName("isTrendUp")
    public boolean isTrendUp() { return isTrendUp; }

    @PropertyName("isTrendUp")
    public void setTrendUp(boolean trendUp) { this.isTrendUp = trendUp; }
}