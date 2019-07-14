package org.mozilla.samples.sync

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.concept.sync.Device

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [DeviceFragment.OnDeviceListInteractionListener] interface.
 */
class DeviceFragment : Fragment() {

    private var listenerDevice: OnDeviceListInteractionListener? = null

    private val adapter = DeviceRecyclerViewAdapter(listenerDevice)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_device_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                adapter = this@DeviceFragment.adapter
            }
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnDeviceListInteractionListener) {
            listenerDevice = context
            adapter.mListenerDevice = context
        } else {
            throw IllegalArgumentException("$context must implement OnDeviceListInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listenerDevice = null
    }

    fun updateDevices(devices: List<Device>) {
        adapter.devices.clear()
        adapter.devices.addAll(devices)
        adapter.notifyDataSetChanged()
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnDeviceListInteractionListener {
        fun onDeviceInteraction(item: Device)
    }
}
