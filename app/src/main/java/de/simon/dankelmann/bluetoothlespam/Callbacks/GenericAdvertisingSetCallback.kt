package de.simon.dankelmann.bluetoothlespam.Callbacks

import android.Manifest
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.util.Log
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck


class GenericAdvertisingSetCallback() : AdvertisingSetCallback() {
    private val _logTag = "GenericAdvertisingSetCallback"
    override fun onAdvertisingSetStarted(advertisingSet: AdvertisingSet?, txPower: Int, status: Int) {

        val context = AppContext.getActivity()

        if (status==AdvertisingSetCallback.ADVERTISE_FAILED_ALREADY_STARTED)
            //Toast.makeText(context, "ADVERTISE_FAILED_ALREADY_STARTED", Toast.LENGTH_SHORT).show();
            Log.d(_logTag, "ADVERTISE_FAILED_ALREADY_STARTED")
        else if (status==AdvertisingSetCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED)
            //Toast.makeText(context, "ADVERTISE_FAILED_FEATURE_UNSUPPORTED", Toast.LENGTH_SHORT).show();
            Log.d(_logTag, "ADVERTISE_FAILED_FEATURE_UNSUPPORTED")
        else if (status==AdvertisingSetCallback.ADVERTISE_FAILED_DATA_TOO_LARGE)
            //Toast.makeText(context, "ADVERTISE_FAILED_DATA_TOO_LARGE", Toast.LENGTH_SHORT).show();
            Log.d(_logTag, "ADVERTISE_FAILED_DATA_TOO_LARGE")
        else if (status==AdvertisingSetCallback.ADVERTISE_FAILED_INTERNAL_ERROR)
            //Toast.makeText(context, "ADVERTISE_FAILED_INTERNAL_ERROR", Toast.LENGTH_SHORT).show();
            Log.d(_logTag, "ADVERTISE_FAILED_INTERNAL_ERROR")
        else if (status==AdvertisingSetCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS)
            //Toast.makeText(context, "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS", Toast.LENGTH_SHORT).show();
            Log.e(_logTag, "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS")
        else if (status==AdvertisingSetCallback.ADVERTISE_SUCCESS)
            //Toast.makeText(context, "ADVERTISE_SUCCESS", Toast.LENGTH_SHORT).show();
            Log.d(_logTag, "ADVERTISE_SUCCESS")


        Log.d(_logTag, "onAdvertisingSetStarted(): txPower:" + txPower + " , status: " + status)

        if(advertisingSet != null){
            if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE, AppContext.getActivity())){
                //advertisingSet!!.setScanResponseData(AdvertiseData.Builder().addServiceUuid(ParcelUuid(UUID.randomUUID())).build())
                //Log.d(_logTag,"adverting set is not null")
            }
        } else {
            Log.d(_logTag,"advertising set is null");
        }
    }

    override fun onAdvertisingDataSet(advertisingSet: AdvertisingSet, status: Int) {
        Log.d(_logTag, "onAdvertisingDataSet() :status:$status")
    }

    override fun onScanResponseDataSet(advertisingSet: AdvertisingSet, status: Int) {
        Log.d(_logTag, "onScanResponseDataSet(): status:$status")
    }

    override fun onAdvertisingSetStopped(advertisingSet: AdvertisingSet) {
        Log.d(_logTag, "onAdvertisingSetStopped():")
    }

}