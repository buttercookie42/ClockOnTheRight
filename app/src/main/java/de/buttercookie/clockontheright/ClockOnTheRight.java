/* Code for moving the statusbar clock to the right based on
 * https://github.com/siavash79/PixelXpert/blob/8b3a6cd70f1ba70772768e8876b1a3b720322902/app/src/main/java/sh/siava/pixelxpert/modpacks/systemui/StatusbarMods.java,
 * licensed under the GPLv3.
 */

package de.buttercookie.clockontheright;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

import android.annotation.SuppressLint;
import android.app.AndroidAppHelper;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Keep;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class ClockOnTheRight implements IXposedHookLoadPackage {

    private Context mContext = null;
    private ViewGroup mStatusBar = null;
    private Object mCollapsedStatusBarFragment = null;
    private LinearLayout mSystemIconArea = null;
    private View mClockView = null;
    int mRightClockPadding = 0;

    @Keep
    public ClockOnTheRight() {}

    @Keep
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.android.systemui")) {
            return;
        }

        Class<?> CollapsedStatusBarFragmentClass = findClassIfExists("com.android.systemui.statusbar.phone.fragment.CollapsedStatusBarFragment", lpparam.classLoader);

        hookAllConstructors(CollapsedStatusBarFragmentClass, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                mCollapsedStatusBarFragment = param.thisObject;
            }
        });

        findAndHookMethod(CollapsedStatusBarFragmentClass,
                "onViewCreated", View.class, Bundle.class, new XC_MethodHook() {
                    @SuppressLint("DiscouragedApi")
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        mContext = AndroidAppHelper.currentApplication();
                        try {
                            mClockView = (View) getObjectField(param.thisObject, "mClockView");
                        } catch (Throwable t) { //PE Plus
                            Object mClockController = getObjectField(param.thisObject, "mClockController");
                            mClockView = (View) callMethod(mClockController, "getClock");
                        }
                        mStatusBar = (ViewGroup) getObjectField(mCollapsedStatusBarFragment, "mStatusBar");
                        mSystemIconArea = mStatusBar.findViewById(mContext.getResources().getIdentifier("statusIcons", "id", mContext.getPackageName()));
                        mRightClockPadding = mContext.getResources().getDimensionPixelSize(mContext.getResources().getIdentifier("status_bar_clock_starting_padding", "dimen", mContext.getPackageName()));

                        moveClock();
                    }
                });
    }

    private void moveClock() {
        ViewGroup parent = (ViewGroup) mClockView.getParent();
        ViewGroup targetArea = (ViewGroup) mSystemIconArea.getParent();
        parent.removeView(mClockView);
        mClockView.setPadding(mRightClockPadding, 0, 0, 0);
        targetArea.addView(mClockView);
    }
}
