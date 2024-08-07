package com.marty.yummy.services.repository;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.marty.yummy.dbutilities.AppDatabase;
import com.marty.yummy.model.FoodDetails;
import com.marty.yummy.services.APIClient;
import com.marty.yummy.services.YummyAPIServices;
import com.marty.yummy.worker.SaveFoodMenu;
import com.marty.yummy.worker.UpdateCart;

import java.util.List;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FoodRepository {

    private static volatile FoodRepository instance;
    private static final String TAG = "FoodRepository";

    private final YummyAPIServices yummyAPIServices = APIClient.getClient().create(YummyAPIServices.class);

    public MutableLiveData<Boolean> getFoodMenu(final Context context) {
        final MutableLiveData<Boolean> isFoodCallOngoing = new MutableLiveData<>();
        isFoodCallOngoing.setValue(true);

        yummyAPIServices.getFoodData().enqueue(new Callback<List<FoodDetails>>() {
            @Override
            public void onResponse(Call<List<FoodDetails>> call, Response<List<FoodDetails>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        AppDatabase db = AppDatabase.getDatabase(context);
                        db.foodDetailsDao().insertAll(response.body());
                        isFoodCallOngoing.postValue(false);
                    });
                } else {
                    Log.e(TAG, "Response not successful: " + response.message());
                    isFoodCallOngoing.setValue(false);
                }
            }

            @Override
            public void onFailure(Call<List<FoodDetails>> call, Throwable t) {
                Log.e(TAG, "Failed to fetch food data: ", t);
                isFoodCallOngoing.setValue(false);
            }
        });
        return isFoodCallOngoing;
    }

    public static FoodRepository getInstance() {
        if (instance == null) {
            synchronized (FoodRepository.class) {
                if (instance == null) {
                    instance = new FoodRepository();
                }
            }
        }
        return instance;
    }

    public void updateCart(final AppDatabase db, FoodDetails foodDetails) {
        Executors.newSingleThreadExecutor().execute(() -> {
            db.cartItemDao().updateCartWithFoodDetails(foodDetails);
        });
    }
}
