package com.marty.yummy.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.marty.yummy.dbutilities.AppDatabase;
import com.marty.yummy.model.FoodDetails;

public class SaveFoodMenu {

    private final AppDatabase db;
    private final List<FoodDetails> foodDetails;

    public SaveFoodMenu(AppDatabase db, List<FoodDetails> foodDetails) {
        this.db = db;
        this.foodDetails = foodDetails;
    }

    public void execute() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            if (db != null) {
                if (foodDetails != null && !foodDetails.isEmpty()) {
                    List<String> nameList = new ArrayList<>();
                    for (FoodDetails foodDetail : foodDetails) {
                        nameList.add(foodDetail.getName());
                        foodDetail.setQuantity(db.cartItemDao().getCartCount(foodDetail.getName()));
                    }
                    db.foodDetailsDao().save(foodDetails);
                    db.foodDetailsDao().deleteOtherFoods(nameList);
                } else {
                    db.foodDetailsDao().deleteAll();
                }
            }
        });
        executor.shutdown();
    }
}
