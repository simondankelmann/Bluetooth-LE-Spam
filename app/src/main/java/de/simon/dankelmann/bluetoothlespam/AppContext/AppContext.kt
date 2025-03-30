package de.simon.dankelmann.bluetoothlespam.AppContext

import android.content.Context

abstract class AppContext {
    companion object {

        // TODO: Migrate away from needing this static field
        private lateinit var _context: Context

        fun setContext(context: Context) {
            _context = context
        }

        fun getContext(): Context {
            return _context
        }
    }
}