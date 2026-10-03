package com.gegaremant.truenasmobile.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Containers.
 *
 * TrueNAS 26.0 removed the whole `virt.*` namespace: `core.get_methods` on a
 * 26.0.0-BETA.3 stand reports zero `virt.*` methods and answers -32601
 * "Method does not exist" for `virt.instance.query`. Containers now live in a
 * dedicated `container.*` namespace, and VMs moved to `vm.*` - so this object
 * describes containers only and no longer carries the VM branch.
 *
 * Every field of [ContainerResponse] is optional with a default. The 26.x record
 * shape could not be observed on the test stand (no containers on it), and a
 * single missing non-null field is enough for Moshi to throw and take the whole
 * screen down - which is exactly how the previous model failed.
 */
object Container {
    enum class Type {
        @field:Json(name = "CONTAINER")
        CONTAINER,
        @field:Json(name = "VM")
        VM
    }
    enum class Status{
        @field:Json(name = "RUNNING")
        RUNNING,
        @field:Json(name = "STOPPED")
        STOPPED,
        @field:Json(name = "STARTING")
        STARTING,
        @field:Json(name = "STOPPING")
        STOPPING,
        @field:Json(name = "UNKNOWN")
        UNKNOWN,
        @field:Json(name = "ERROR")
        ERROR,
        @field:Json(name = "FROZEN")
        FROZEN,
        @field:Json(name = "FREEZING")
        FREEZING,
        @field:Json(name = "THAWED")
        THAWED,
        @field:Json(name = "ABORTING")
        ABORTING
    }
    enum class AliasType{
        @field:Json(name = "INET")
        INET,
        @field:Json(name = "INET6")
        INET6
    }
    enum class RootDiskIOBus(string: String) {
        @field:Json(name = "NVME")
        NVME("NVME"),
        @field:Json(name = "VIRTIO-BLK")
        VIRTIO_BLK("VIRTIO_BLK"),
        @field:Json(name = "VIRTIO-SCSI")
        VIRTIO_SCSI("VIRTIO-SCSI")
    }
    data class Aliases(
        val type : AliasType,
        val address : String,
        val netmask: Int? = null
    )
    data class Image(
        val architecture: String? = null,
        val description: String? = null,
        val os: String? = null,
        val release :String? = null,
        val serial :String? = null,
        val type : String? = null,
        val variant: String? = null,
        val secureboot : Boolean? = false
    )
    data class Uid(
        val hostid : Int,
        val maprange : Int,
        val nsid : Int,
    )
    data class Gid(
        val hostid : Int,
        val maprange : Int,
        val nsid : Int,
    )
    data class UsernsIdmap(
        val uid: Uid,
        val gid: Gid
    )
    // container.query
    @Suppress("PropertyName")
    @JsonClass(generateAdapter = true)
    data class ContainerResponse(
        val id: String,
        val name: String = "",
        // 26.x reports the runtime state under `state`; older payloads used
        // `status`. Both are optional and [resolvedStatus] prefers `state`.
        val state: String? = null,
        val status: String? = null,
        val type: Type = Type.CONTAINER,
        val cpu: String? = null,
        val memory: Int? = null,
        val autostart: Boolean? = null,
        val environment: Map<String, String>? = null,
        val aliases: List<Aliases>? = null,
        val image: Image? = null,
        val userns_idmap: UsernsIdmap? = null,
        val raw: Map<Any, Any>? = null,
        val vnc_enabled: Boolean? = null,
        val vnc_port: Int? = null,
        val vnc_password: String? = null,
        val secure_boot: Boolean? = null,
        val root_disk_size: Int? = null,
        val root_disk_io_bus: RootDiskIOBus? = null,
        val storage_pool: String? = null,
    ) {
        /**
         * Runtime state as the UI understands it.
         *
         * Reads `state` first and falls back to `status`, matching by enum
         * name and case so an unrecognised value degrades to [Status.UNKNOWN]
         * instead of throwing.
         */
        val resolvedStatus: Status
            get() {
                val raw = state ?: status
                return Status.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
                    ?: Status.UNKNOWN
            }

        val aliasesOrEmpty: List<Aliases> get() = aliases.orEmpty()
    }

    // container.update
    @Suppress("PropertyName")
    data class ContainerUpdate(
        val environment: Map<String,String>? = null,
        val autostart : Boolean? = null,
        val cpu: String? = null,
        val memory: Int? = null,
        val vnc_port : Int? = null, // remember to set UI to limit this to between  greater or equal to 5900 and lesser or equal to 65535
        val vnc_enabled : Boolean,
        val vnc_password : String? = null,
        val secure_boot: Boolean,
        val root_disk_size: Int? = null, // set this to greater or equal to 5
        val root_disk_io_bus: RootDiskIOBus? = null,
    )
    enum class InstanceType(string: String){
        @field:Json(name = "CONTAINER")
        CONTAINER("CONTAINER"),
        @field:Json(name = "VM")
        VM("VM")
    }

    // Image repo Query
    // Provide choices for instance image from a remote repository.
    @Suppress("PropertyName")
    @JsonClass(generateAdapter = true)
    data class ImageQueryResponse(
        val label : String,
        val os : String,
        val release: String,
        val archs : List<String>,
        val variant: String,
        val instance_types: List<InstanceType>,
        val secureboot: Boolean?
    )

    // Device responses
    @Suppress("PropertyName")
    data class DiskDevice(
        val name: String? = null,
        val description: String? = null,
        val readonly : Boolean? = false,
        val dev_type: String,
        val source: String? = null,
        val destination : String? = null,
        val boot_priority : Int? = null,
        val io_bus: RootDiskIOBus? = null,
        val storage_pool: String? = null
    ): Device
    @Suppress("PropertyName")
    data class GpuDevice(
        val name: String? = null,
        val description: String? = null,
        val readonly : Boolean? = false,
        val dev_type: String,
        val gpu_type: String,
        val id : String? = null,
        val gid : Int? = null,
        val uid: Int? = null,
        val mode : String? = null,
        val mdev : String? = null,
        val mig_uuid: String? = null,
        val pci : String? = null,
        val productid: String? = null,
        val vendorid: String? = null,
    ): Device
    @Suppress("PropertyName")
    data class ProxyDevice(
        val name: String? = null,
        val description: String? = null,
        val readonly : Boolean? = false,
        val dev_type: String,
        val source_proto: String,
        val source_port: Int,
        val dest_proto: String,
        val dest_port: Int,
    ): Device
    @Suppress("PropertyName")
    data class TPMDevice(
        val name: String? = null,
        val description: String? = null,
        val readonly : Boolean? = false,
        val dev_type: String,
        val path: String,
        val pathrm : String? = null
    ): Device
    @Suppress("PropertyName")
    data class USBDevice(
        val name: String? = null,
        val description: String? = null,
        val readonly : Boolean? = false,
        val dev_type: String,
        val bus: Int? = null,
        val dev: Int? = null,
        val product_id :String? = null,
        val vendor_id : String? = null,
    ): Device
    @Suppress("PropertyName")
    data class NICDevice(
        val name: String? = null,
        val description: String? = null,
        val readonly : Boolean? = false,
        val dev_type: String,
        val network: String? = null,
        val nic_type: String? = null,
        val parent :String? = null,
    ): Device
    @Suppress("PropertyName")
    data class PCIDevice(
        val name: String? = null,
        val description: String? = null,
        val readonly : Boolean? = false,
        val dev_type: String,
        val address: String,
    ): Device
    sealed interface Device

    /** `alpine:latest` -> `alpine` + `latest`; a bare name means `latest`. */
    fun parseImageReference(reference: String): CreateImage {
        val withoutRegistry = reference.trim()
            .removePrefix("docker.io/")
            .removePrefix("library/")
            .substringAfterLast('/')
        val colon = withoutRegistry.lastIndexOf(':')
        return if (colon > 0) {
            CreateImage(
                name = withoutRegistry.substring(0, colon),
                version = withoutRegistry.substring(colon + 1)
            )
        } else {
            CreateImage(name = withoutRegistry, version = "latest")
        }
    }

    /**
     * Arguments of `container.create` in the 26.0 API.
     *
     * Verified on a 26.0 stand, one field at a time:
     * - a plain image string is rejected - `image` must be an object;
     * - the object wants `name` and `version` separately, and passing
     *   `reference` or `docker_registry` fails with `Extra inputs are not
     *   permitted`;
     * - `pool` is required as well (`Either configure a preferred pool in lxc
     *   settings or provide a pool name`), unless the stand has a preferred pool.
     *
     * The call returns a job id, so the container only exists once that job
     * succeeds - and a real stand then pulls the image, which is why the first
     * creation can take minutes.
     */
    @JsonClass(generateAdapter = true)
    data class createArgs(
        val name: String,
        val image: CreateImage,
        val pool: String
    )

    @JsonClass(generateAdapter = true)
    data class CreateImage(
        val name: String,
        val version: String
    )

    @JsonClass(generateAdapter = true)
    data class stopArgs(
        @field:Json("timeout")
        val timeout: Int? = -1,
        @field:Json("force")
        val force: Boolean? = false
    )

    @JsonClass(generateAdapter = true)
    @Suppress("PropertyName")
    /**
     * Maps to call :
     * @see com.gegaremant.truenasmobile.data.api.ApiMethods.Virt.GET_IMAGE_CHOICES
     */
    data class ImageChoice(
        val label: String,
        val os :String,
        val release: String,
        val archs : List<String>,
        val variant: String,
        val instance_types: List<String>,
        val secureboot: Boolean? = null
    )
}