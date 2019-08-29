package com.njdc.mapkt.base

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject

class RxBus private constructor() {
    private val mBus: Subject<Any> by lazy {
        PublishSubject.create<Any>().toSerialized()
    }

    companion object {
        fun getInstance() = Holder.holder
    }

    private object Holder {
        val holder = RxBus()
    }

    fun <T> toObservable(clzz: Class<T>): Observable<T> = mBus.ofType(clzz)

    fun post(obj: Any) {
        mBus.onNext(obj)
    }

    fun hasObservers(): Boolean {
        return mBus.hasObservers()
    }

}