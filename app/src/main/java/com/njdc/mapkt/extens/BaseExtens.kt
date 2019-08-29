package com.njdc.mapkt.extens

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.uber.autodispose.AutoDispose
import com.uber.autodispose.ObservableSubscribeProxy
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

fun <T> Observable<T>.async(): Observable<T> =
    this.subscribeOn(Schedulers.io()).observeOn(
        AndroidSchedulers.mainThread()
    )

fun <T> Observable<T>.sync(): Observable<T> =
    this.subscribeOn(AndroidSchedulers.mainThread()).observeOn(
        AndroidSchedulers.mainThread()
    )

fun <T> Observable<T>.bindLifeCycle(owner: LifecycleOwner): ObservableSubscribeProxy<T> =
    this.`as`(
        AutoDispose.autoDisposable(
            AndroidLifecycleScopeProvider.from(
                owner,
                Lifecycle.Event.ON_DESTROY
            )
        )
    )

fun Activity.toast(msg: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, msg, duration).show()
}

fun View.visible(isVisible: Boolean): View {
    visibility = if (isVisible) View.VISIBLE else View.GONE
    return this
}

fun formatFloorName(string: String): String {
    var s: String = string
    if (s.indexOf(".") > 0) {
        s = s.replace("0+?$".toRegex(), "")
        s = s.replace("[.]$".toRegex(), "")
    }
    return s.plus("F")
}

fun View.hideKeyboard() {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}
