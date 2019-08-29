package com.njdc.mapkt.base

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

abstract class BaseActivity : AppCompatActivity(), LifecycleOwner {

    private val mLifecycleRegistry: LifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }

    override fun getLifecycle(): Lifecycle {
        return mLifecycleRegistry
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        mLifecycleRegistry.markState(Lifecycle.State.CREATED)
        super.onCreate(savedInstanceState, persistentState)
    }
}