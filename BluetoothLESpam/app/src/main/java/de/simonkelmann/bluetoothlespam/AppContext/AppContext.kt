package de.simonkelmann.bluetoothlespam.AppContext

import android.app.Activity
import android.content.Context

abstract class AppContext {
    companion object {

        private lateinit var context: Context
        private lateinit var activity: Activity

        fun setContext(con: Context) {
            context=con
        }

        fun getContext():Context {
            return context
        }

        fun setActivity(act: Activity) {
            activity=act
        }

        fun getActivity():Activity {
            return activity
        }

        fun registerPermissionCallback(requestCode: Int, callback:Runnable){

        }

    }
}