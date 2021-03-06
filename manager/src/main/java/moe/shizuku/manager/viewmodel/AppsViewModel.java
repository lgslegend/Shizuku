package moe.shizuku.manager.viewmodel;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import moe.shizuku.manager.BuildConfig;
import moe.shizuku.manager.authorization.AuthorizationManager;
import moe.shizuku.manager.utils.AppNameComparator;

public class AppsViewModel extends SharedViewModel {

    private final MutableLiveData<DataWrapper<List<PackageInfo>>> mPackages = new MutableLiveData<>();
    private final MutableLiveData<DataWrapper<Integer>> mGrantedCount = new MutableLiveData<>();
    private Disposable mPackagesDisposable;
    private Disposable mCountDisposable;

    public MutableLiveData<DataWrapper<List<PackageInfo>>> getPackages() {
        return mPackages;
    }

    public MutableLiveData<DataWrapper<Integer>> getGrantedCount() {
        return mGrantedCount;
    }

    @Override
    protected void onFullyCleared() {
        if (mPackagesDisposable != null)
            mPackagesDisposable.dispose();

        if (mCountDisposable != null)
            mCountDisposable.dispose();
    }

    public void load(Context context) {
        mPackagesDisposable = Single
                .fromCallable(() -> {
                    List<PackageInfo> list = new ArrayList<>();
                    List<String> packages = new ArrayList<>();
                    for (String packageName : AuthorizationManager.getPackages()) {
                        if (BuildConfig.APPLICATION_ID.equals(packageName))
                            continue;

                        try {
                            list.add(context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA));
                        } catch (PackageManager.NameNotFoundException ignored) {
                        }

                        if (AuthorizationManager.granted(packageName))
                            packages.add(packageName);
                    }
                    Collections.sort(list, new AppNameComparator(context).getPackageInfoComparator());
                    return new List[]{list, packages};
                })
                .subscribeOn(Schedulers.io())
                .subscribe(lists -> {
                    //noinspection unchecked
                    mPackages.postValue(new DataWrapper<>(lists[0]));
                    mGrantedCount.postValue(new DataWrapper<>(lists[1].size()));
                }, throwable -> {
                    mPackages.postValue(new DataWrapper<>(throwable));
                    mGrantedCount.postValue(new DataWrapper<>(throwable));
                });
    }

    public void loadCount() {
        mCountDisposable = Single
                .fromCallable(() -> {
                    int count = 0;
                    for (String packageName : AuthorizationManager.getPackages()) {
                        if (BuildConfig.APPLICATION_ID.equals(packageName))
                            continue;

                        count += AuthorizationManager.granted(packageName) ? 1 : 0;
                    }
                    return count;
                })
                .subscribeOn(Schedulers.io())
                .subscribe(count -> mGrantedCount.postValue(new DataWrapper<>(count)), throwable -> {
                    mGrantedCount.postValue(new DataWrapper<>(throwable));
                });
    }
}
