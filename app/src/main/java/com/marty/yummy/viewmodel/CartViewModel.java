package com.marty.yummy.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.marty.yummy.dbutilities.AppDatabase;
import com.marty.yummy.model.CartItem;
import com.marty.yummy.utility.ObservableObject;

import java.util.List;

import static com.marty.yummy.ui.HomeScreenActivity.INTENT_UPDATE_LIST;

public class CartViewModel extends AndroidViewModel {

    private AppDatabase db;
    private double totalCost = 0.0, discount = 0.0, deliveryCost = 0.0;
    private final MutableLiveData<Double> grandTotal = new MutableLiveData<>();
    private final MediatorLiveData<List<CartItem>> mediatorLiveData = new MediatorLiveData<>();
    private String couponApplied = "";
    private final MutableLiveData<String> errorString = new MutableLiveData<>();

    public CartViewModel(@NonNull Application application) {
        super(application);
        init();
    }

    private void init() {
        db = AppDatabase.getDatabase(getApplication().getApplicationContext());
        subscribeToCartChanges();
    }

    private void subscribeToCartChanges() {
        LiveData<List<CartItem>> cartItemsLiveData = db.cartItemDao().getCartItems();
        mediatorLiveData.addSource(cartItemsLiveData, cartItems -> {
            mediatorLiveData.setValue(cartItems);
            calculateGrandTotalCost();
        });
    }

    private void calculateGrandTotalCost() {
        List<CartItem> cartItemList = mediatorLiveData.getValue();
        totalCost = 0.0;
        if (cartItemList != null) {
            for (CartItem cartItem : cartItemList) {
                totalCost += cartItem.getPrice() * cartItem.getQuantity();
            }
            discount = calculateDiscount(couponApplied);
            deliveryCost = calculateDeliveryCost(couponApplied);
            grandTotal.setValue(totalCost - discount + deliveryCost);
        }
    }

    private double calculateDeliveryCost(String couponApplied) {
        if ("FREEDEL".equals(couponApplied) && totalCost > 100) {
            errorString.setValue("");
            return 0.0;
        } else if ("FREEDEL".equals(couponApplied)) {
            errorString.setValue("Cart value should be > 100");
        }
        return 30.0;
    }

    private double calculateDiscount(String couponApplied) {
        if ("F22LABS".equals(couponApplied) && totalCost > 400) {
            errorString.setValue("");
            return (totalCost * 20) / 100;
        } else if ("F22LABS".equals(couponApplied)) {
            errorString.setValue("Cart value should be > 400");
        }
        return 0.0;
    }

    public double getDiscountAmt() {
        return discount;
    }

    public double getDeliveryCost() {
        return deliveryCost;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public LiveData<Double> getGrandTotal() {
        return grandTotal;
    }

    public void applyCoupon(String coupon) {
        couponApplied = coupon;
        calculateGrandTotalCost();
    }

    public MediatorLiveData<List<CartItem>> getCartItemsLiveData() {
        return mediatorLiveData;
    }

    public void removeItem(String name) {
        // Ideally, this should be done using a background thread or Coroutines.
        new Thread(() -> {
            db.cartItemDao().deleteCartItem(name);
            ObservableObject.getInstance().updateValue(new Intent(INTENT_UPDATE_LIST));
        }).start();
    }

    public MutableLiveData<String> getErrorString() {
        return errorString;
    }
}
