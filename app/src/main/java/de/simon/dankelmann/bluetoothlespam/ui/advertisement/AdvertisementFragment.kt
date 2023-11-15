package de.simon.dankelmann.bluetoothlespam.ui.advertisement

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import de.simon.dankelmann.bluetoothlespam.Models.AdvertisementSetCollection
import de.simon.dankelmann.bluetoothlespam.databinding.FragmentAdvertisementBinding
import kotlin.reflect.typeOf


class AdvertisementFragment : Fragment() {

    private val _logTag = "AdvertisementFragment"
    private var _viewModel: AdvertisementViewModel? = null
    private var _binding: FragmentAdvertisementBinding? = null
    private var _advertisementSetCollection: AdvertisementSetCollection? = null

    companion object {
        fun newInstance() = AdvertisementFragment()
    }

    private lateinit var viewModel: AdvertisementViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val viewModel = ViewModelProvider(this)[AdvertisementViewModel::class.java]
        _viewModel = viewModel
        _binding = FragmentAdvertisementBinding.inflate(inflater, container, false)
        val root: View = _binding!!.root

        if(arguments != null){
            var advertisementSetCollectionArgumentKey = "advertisementSetCollection"

            var advertismentSetCollection = AdvertisementSetCollection()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val type: Class<AdvertisementSetCollection> = AdvertisementSetCollection::class.java
                var collectionFromBundle = requireArguments().getSerializable(advertisementSetCollectionArgumentKey, type)
                if(collectionFromBundle != null){
                    advertismentSetCollection = collectionFromBundle
                }
            } else {
                var collectionFromBundle = requireArguments().getSerializable(advertisementSetCollectionArgumentKey)
                if(collectionFromBundle != null){
                    advertismentSetCollection = collectionFromBundle as AdvertisementSetCollection
                }
            }

            Log.d(_logTag," ${advertismentSetCollection.advertisementSetLists.count()}")
            advertismentSetCollection.advertisementSetLists.forEach {
                Log.d(_logTag," ${it.title} ${it.advertisementSets.count()}")
            }
        }

        setupUi()

        return root//inflater.inflate(R.layout.fragment_advertisement, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(AdvertisementViewModel::class.java)
        // TODO: Use the ViewModel
    }

    fun setupUi(){

    }
}