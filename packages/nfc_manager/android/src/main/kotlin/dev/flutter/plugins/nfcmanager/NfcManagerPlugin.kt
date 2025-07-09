package dev.flutter.plugins.nfcmanager

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcBarcode
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.nfc.tech.TagTechnology
import android.os.Build
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import java.util.*

class NfcManagerPlugin: FlutterPlugin, ActivityAware, HostApiPigeon, BroadcastReceiver() {
  private lateinit var flutterApi: FlutterApiPigeon
  private lateinit var activity: Activity
  private var adapter: NfcAdapter? = null
  private var cachedTags: MutableMap<String, Tag> = mutableMapOf()
  private var connectedTech: TagTechnology? = null

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    HostApiPigeon.setUp(flutterPluginBinding.binaryMessenger, this)
    flutterApi = FlutterApiPigeon(flutterPluginBinding.binaryMessenger)
    adapter = NfcAdapter.getDefaultAdapter(flutterPluginBinding.applicationContext)
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    HostApiPigeon.setUp(binding.binaryMessenger, null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      activity.applicationContext.registerReceiver(this, null, RECEIVER_NOT_EXPORTED)
    } else {
      activity.applicationContext.registerReceiver(this, null)
    }
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivityForConfigChanges() {
    // noop
  }

  override fun onDetachedFromActivity() {
    // noop
  }

  override fun onReceive(context: Context?, intent: Intent?) {
    intent ?: run { return }
    if (intent.action != NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) { return }
    val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF)
    flutterApi.onAdapterStateChanged(toAdapterStatePigeon(state)) { /* noop */ }
  }

  override fun nfcAdapterIsEnabled(): Boolean {
    return getAdapter().isEnabled
  }

  override fun nfcAdapterIsSecureNfcEnabled(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { throw FlutterError("unavailable", "Required Android SDK >= ${Build.VERSION_CODES.Q}.", null) }
    return getAdapter().isSecureNfcEnabled
  }

  override fun nfcAdapterIsSecureNfcSupported(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) { throw FlutterError("unavailable", "Required Android SDK >= ${Build.VERSION_CODES.Q}.", null) }
    return getAdapter().isSecureNfcSupported
  }

  override fun nfcAdapterEnableReaderMode(flags: List<ReaderFlagPigeon>) {
    getAdapter().enableReaderMode(activity, { onTagDiscovered(it) }, toInt(flags), null)
  }

  override fun nfcAdapterDisableReaderMode() {
    getAdapter().disableReaderMode(activity)
    cachedTags.clear() // Consider when to remove the tag.
  }

  override fun ndefGetNdefMessage(handle: String): NdefMessagePigeon? {
    val tech = forceConnect(handle) { Ndef.get(it) }
    val message = tech.ndefMessage
    return if (message != null) toNdefMessagePigeon(message) else null
  }

  override fun ndefWriteNdefMessage(handle: String, message: NdefMessagePigeon) {
    val tech = forceConnect(handle) { Ndef.get(it) }
    tech.writeNdefMessage(toNdefMessage(message))
  }

  override fun ndefMakeReadOnly(handle: String): Boolean {
    val tech = forceConnect(handle) { Ndef.get(it) }
    return tech.makeReadOnly()
  }

  override fun nfcAGetMaxTransceiveLength(handle: String): Long {
    val tech = forceConnect(handle) { NfcA.get(it) }
    return tech.maxTransceiveLength.toLong()
  }

  override fun nfcAGetTimeout(handle: String): Long {
    val tech = forceConnect(handle) { NfcA.get(it) }
    return tech.timeout.toLong()
  }

  override fun nfcASetTimeout(handle: String, timeout: Long) {
    val tech = forceConnect(handle) { NfcA.get(it) }
    tech.timeout = timeout.toInt()
  }

  override fun nfcATransceive(handle: String, bytes: ByteArray): ByteArray {
    val tech = forceConnect(handle) { NfcA.get(it) }
    return tech.transceive(bytes)
  }

  override fun nfcBGetMaxTransceiveLength(handle: String): Long {
    val tech = forceConnect(handle) { NfcB.get(it) }
    return tech.maxTransceiveLength.toLong()
  }

  override fun nfcBTransceive(handle: String, bytes: ByteArray): ByteArray {
    val tech = forceConnect(handle) { NfcB.get(it) }
    return tech.transceive(bytes)
  }

  override fun nfcFGetMaxTransceiveLength(handle: String): Long {
    val tech = forceConnect(handle) { NfcF.get(it) }
    return tech.maxTransceiveLength.toLong()
  }

  override fun nfcFGetTimeout(handle: String): Long {
    val tech = forceConnect(handle) { NfcF.get(it) }
    return tech.timeout.toLong()
  }

  override fun nfcFSetTimeout(handle: String, timeout: Long) {
    val tech = forceConnect(handle) { NfcF.get(it) }
    tech.timeout = timeout.toInt()
  }

  override fun nfcFTransceive(handle: String, bytes: ByteArray): ByteArray {
    val tech = forceConnect(handle) { NfcF.get(it) }
    return tech.transceive(bytes)
  }

  override fun nfcVGetMaxTransceiveLength(handle: String): Long {
    val tech = forceConnect(handle) { NfcV.get(it) }
    return tech.maxTransceiveLength.toLong()
  }

  override fun nfcVTransceive(handle: String, bytes: ByteArray): ByteArray {
    val tech = forceConnect(handle) { NfcV.get(it) }
    return tech.transceive(bytes)
  }

  override fun isoDepGetMaxTransceiveLength(handle: String): Long {
    val tech = forceConnect(handle) { IsoDep.get(it) }
    return tech.maxTransceiveLength.toLong()
  }

  override fun isoDepGetTimeout(handle: String): Long {
    val tech = forceConnect(handle) { IsoDep.get(it) }
    return tech.timeout.toLong()
  }

  override fun isoDepSetTimeout(handle: String, timeout: Long) {
    val tech = forceConnect(handle) { IsoDep.get(it) }
    tech.timeout = timeout.toInt()
  }

  override fun isoDepTransceive(handle: String, bytes: ByteArray): ByteArray {
    val tech = forceConnect(handle) { IsoDep.get(it) }
    return tech.transceive(bytes)
  }

  override fun mifareClassicGetMaxTransceiveLength(handle: String): Long {
    val tech = forceConnect(handle) { MifareClassic.get(it) }
    return tech.maxTransceiveLength.toLong()
  }

  override fun mifareClassicGetTimeout(handle: String): Long {
    val tech = forceConnect(handle) { MifareClassic.get(it) }
    return tech.timeout.toLong()
  }

  override fun mifareClassicSetTimeout(handle: String, timeout: Long) {
    val tech = forceConnect(handle) { MifareClassic.get(it) }
    tech.timeout = timeout.toInt()
  }

  override fun mifareClassicAuthenticateSectorWithKeyA(handle: String, sectorIndex: Long, key: ByteArray): Boolean {
    val tech = forceConnect(handle) { MifareClassic.get(it) }
    return tech.authenticateSectorWithKeyA(sectorIndex.toInt(), key)
  }

  override fun mifareClassicAuthenticateSectorWithKeyB(handle: String, sectorIndex: Long, key: ByteArray): Boolean {
    val tech = forceConnect(handle) { MifareClassic.get(it) }
    return tech.authenticateSectorWithKeyB(sectorIndex.toInt(), key)
  }

  override fun mifareClassicGetBlockCountInSector(handle: String, sectorIndex: Long): Long {
    val tech = forceConnect(handle) { MifareClassic.get(it) }
    return tech.getBlockCountInSector(sectorIndex.toInt()).toLong()
  }

  override fun mifareClassicBlockToSector(handle: String, blockIndex: Long): Long {
    val tech = forceConnect(handle) { MifareClassic.get(it) }
    return tech.blockToSector(blockIndex.toInt()).toLong()
  }

  override fun mifareClassicSectorToBlock(handle: String, sectorIndex: Long): Long {
    val tech = forceConnect(handle) { MifareClassic.get(it) }
    return tech.sectorToBlock(sectorIndex.toInt()).toLong()
  }

  override fun mifareClassicIncrement(handle: String, blockIndex: Long, value: Long) {
    val tech = forceConnect(handle) { MifareClassic.get(it) }
    tech.increment(blockIndex.toInt(), value.toInt())
  }

  override fun mifareClassicDecrement(handle: String, blockIndex: Long, value: Long) {
    val tech = forceConnect(handle) { MifareClassic.get(it) }
    tech.decrement(blockIndex.toInt(), value.toInt())
  }

  override fun mifareClassicRestore(handle: String, blockIndex: Long) {
    val tech = forceConnect(handle) { MifareClassic.get(it) }
    tech.restore(blockIndex.toInt())
  }

  override fun mifareClassicTransfer(handle: String, blockIndex: Long) {
    val tech = forceConnect(handle) { MifareClassic.get(it) }
    tech.transfer(blockIndex.toInt())
  }

  override fun mifareClassicReadBlock(handle: String, blockIndex: Long): ByteArray {
    val tech = forceConnect(handle) { MifareClassic.get(it) }
    return tech.readBlock(blockIndex.toInt())
  }

  override fun mifareClassicWriteBlock(handle: String, blockIndex: Long, data: ByteArray) {
    val tech = forceConnect(handle) { MifareClassic.get(it) }
    tech.writeBlock(blockIndex.toInt(), data)
  }

  override fun mifareClassicTransceive(handle: String, bytes: ByteArray): ByteArray {
    val tech = forceConnect(handle) { MifareClassic.get(it) }
    return tech.transceive(bytes)
  }

  override fun mifareUltralightGetMaxTransceiveLength(handle: String): Long {
    val tech = forceConnect(handle) { MifareUltralight.get(it) }
    return tech.maxTransceiveLength.toLong()
  }

  override fun mifareUltralightGetTimeout(handle: String): Long {
    val tech = forceConnect(handle) { MifareUltralight.get(it) }
    return tech.timeout.toLong()
  }

  override fun mifareUltralightSetTimeout(handle: String, timeout: Long) {
    val tech = forceConnect(handle) { MifareUltralight.get(it) }
    tech.timeout = timeout.toInt()
  }

  override fun mifareUltralightReadPages(handle: String, pageOffset: Long): ByteArray {
    val tech = forceConnect(handle) { MifareUltralight.get(it) }
    return tech.readPages(pageOffset.toInt())
  }

  override fun mifareUltralightWritePage(handle: String, pageOffset: Long, data: ByteArray) {
    val tech = forceConnect(handle) { MifareUltralight.get(it) }
    tech.writePage(pageOffset.toInt(), data)
  }

  override fun mifareUltralightTransceive(handle: String, bytes: ByteArray): ByteArray {
    val tech = forceConnect(handle) { MifareUltralight.get(it) }
    return tech.transceive(bytes)
  }

  override fun ndefFormatableFormat(handle: String, firstMessage: NdefMessagePigeon) {
    val tech = forceConnect(handle) { NdefFormatable.get(it) }
    tech.format(toNdefMessage(firstMessage))
  }

  override fun ndefFormatableFormatReadOnly(handle: String, firstMessage: NdefMessagePigeon) {
    val tech = forceConnect(handle) { NdefFormatable.get(it) }
    tech.formatReadOnly(toNdefMessage(firstMessage))
  }

  private fun onTagDiscovered(tag: Tag) {
    val handle = UUID.randomUUID().toString()
    val tagMap = toTagMap(tag, handle)
    cachedTags[handle] = tag
    activity.runOnUiThread { flutterApi.onTagDiscovered(tagMap) { /* no op */ } }
  }

  private fun getAdapter(): NfcAdapter {
    return adapter ?: run { throw FlutterError("not_supported", "NFC is not supported on this device.", null) }
  }

  private inline fun <reified T: TagTechnology> forceConnect(handle: String, getMethod: (Tag) -> T?): T {
    val tag = cachedTags[handle] ?: run { throw FlutterError("tag_not_found", "You may have disable the session.", null) }
    val tech = getMethod(tag) ?: run { throw FlutterError("tag_not_found", "The tag cannot be converted to ${T::class.java.name}.", null) }
    val connectedTech = connectedTech ?: run {
      tech.connect()
      connectedTech = tech
      return tech
    }
    if (connectedTech.tag != tech.tag || connectedTech::class.java.name != tech::class.java.name) {
      try { connectedTech.close() } catch (e: Exception) { /* no op */ }
      tech.connect()
      this.connectedTech = tech
      return tech
    }
    return tech
  }
}

private fun toInt(value: List<ReaderFlagPigeon>): Int {
  return value.fold(0) { p, e -> p or toInt(e)}
}

private fun toInt(value: ReaderFlagPigeon): Int {
  return when (value) {
    ReaderFlagPigeon.NFC_A-> NfcAdapter.FLAG_READER_NFC_A
    ReaderFlagPigeon.NFC_B -> NfcAdapter.FLAG_READER_NFC_B
    ReaderFlagPigeon.NFC_BARCODE -> NfcAdapter.FLAG_READER_NFC_BARCODE
    ReaderFlagPigeon.NFC_F -> NfcAdapter.FLAG_READER_NFC_F
    ReaderFlagPigeon.NFC_V -> NfcAdapter.FLAG_READER_NFC_V
    ReaderFlagPigeon.NO_PLATFORM_SOUNDS -> NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
    ReaderFlagPigeon.SKIP_NDEF_CHECK -> NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
  }
}

private fun toNdefMessage(value: NdefMessagePigeon): NdefMessage {
  return NdefMessage(value.records.map { toNdefRecord(it) }.toTypedArray())
}

private fun toNdefMessagePigeon(value: NdefMessage): NdefMessagePigeon {
  return NdefMessagePigeon(
    records = value.records.map { toNdefRecordPigeon(it) }
  )
}

private fun toNdefRecord(value: NdefRecordPigeon): NdefRecord {
  return NdefRecord(
    toShort(value.tnf),
    value.type,
    value.id,
    value.payload,
  )
}

private fun toNdefRecordPigeon(value: NdefRecord): NdefRecordPigeon {
  return NdefRecordPigeon(
    tnf = toTypeNameFormatPigeon(value.tnf),
    type = value.type,
    id = value.id,
    payload = value.payload,
  )
}

private fun toTagMap(value: Tag, handle: String): Map<String, Any> {
  val techList = value.techList.map { it.split('.').last() }
  val tagMap = mutableMapOf<String, Any>(
    "handle" to handle,
    "id" to value.id,
    "techList" to techList,
  )

  if (techList.contains("Ndef")) {
    tagMap["ndef"] = Ndef.get(value)?.let { toNdefMap(it) }!!
  }
  if (techList.contains("NfcA")) {
    tagMap["nfca"] = NfcA.get(value)?.let { toNfcAMap(it) }!!
  }
  if (techList.contains("NfcB")) {
    tagMap["nfcb"] = NfcB.get(value)?.let { toNfcBMap(it) }!!
  }
  if (techList.contains("NfcF")) {
    tagMap["nfcf"] = NfcF.get(value)?.let { toNfcFMap(it) }!!
  }
  if (techList.contains("NfcV")) {
    tagMap["nfcv"] = NfcV.get(value)?.let { toNfcVMap(it) }!!
  }
  if (techList.contains("IsoDep")) {
    tagMap["isodep"] = IsoDep.get(value)?.let { toIsoDepMap(it) }!!
  }
  if (techList.contains("MifareClassic")) {
    tagMap["mifareclassic"] = MifareClassic.get(value)?.let { toMifareClassicMap(it) }!!
  }
  if (techList.contains("MifareUltralight")) {
    tagMap["mifareultralight"] = MifareUltralight.get(value)?.let { toMifareUltralightMap(it) }!!
  }
  if (techList.contains("NdefFormatable")) {
    tagMap["ndefFormatable"] = NdefFormatable.get(value)?.let { "" }!!
  }
  if (techList.contains("NfcBarcode")) {
    tagMap["nfcbarcode"] = NfcBarcode.get(value)?.let { toNfcBarcodeMap(it) }!!
  }

  return tagMap
}

private fun toNdefMap(value: Ndef): Map<String, Any?> {
  return mapOf(
    "type" to value.type,
    "isWritable" to value.isWritable,
    "maxSize" to value.maxSize.toLong(),
    "canMakeReadOnly" to value.canMakeReadOnly(),
    "cachedNdefMessage" to value.cachedNdefMessage?.let { toNdefMessageMap(it) },
  )
}

private fun toNdefMessageMap(value: NdefMessage): Map<String, Any?> {
  return mapOf(
    "records" to value.records.map { toNdefRecordMap(it) }
  )
}

private fun toNdefRecordMap(value: NdefRecord): Map<String, Any?> {
  return mapOf(
    "tnf" to toTypeNameFormatPigeon(value.tnf).raw,
    "type" to value.type,
    "id" to value.id,
    "payload" to value.payload,
  )
}

private fun toNfcAMap(value: NfcA): Map<String, Any?> {
  return mapOf(
    "atqa" to value.atqa,
    "sak" to value.sak.toLong(),
  )
}

private fun toNfcBMap(value: NfcB): Map<String, Any?> {
  return mapOf(
    "applicationData" to value.applicationData,
    "protocolInfo" to value.protocolInfo,
  )
}

private fun toNfcFMap(value: NfcF): Map<String, Any?> {
  return mapOf(
    "manufacturer" to value.manufacturer,
    "systemCode" to value.systemCode,
  )
}

private fun toNfcVMap(value: NfcV): Map<String, Any?> {
  return mapOf(
    "dsfId" to value.dsfId.toLong(),
    "responseFlags" to value.responseFlags.toLong(),
  )
}

private fun toIsoDepMap(value: IsoDep): Map<String, Any?> {
  return mapOf(
    "hiLayerResponse" to value.hiLayerResponse,
    "historicalBytes" to value.historicalBytes,
    "isExtendedLengthApduSupported" to value.isExtendedLengthApduSupported,
  )
}

private fun toMifareClassicMap(value: MifareClassic): Map<String, Any?> {
  return mapOf(
    "type" to toMifareClassicTypePigeon(value.type).raw,
    "blockCount" to value.blockCount.toLong(),
    "sectorCount" to value.sectorCount.toLong(),
    "size" to value.size.toLong(),
  )
}

private fun toMifareUltralightMap(value: MifareUltralight): Map<String, Any?> {
  return mapOf(
    "type" to toMifareUltralightTypePigeon(value.type).raw
  )
}

private fun toNfcBarcodeMap(value: NfcBarcode): Map<String, Any?> {
  return mapOf(
    "type" to toNfcBarcodeTypePigeon(value.type).raw,
    "barcode" to value.barcode,
  )
}

private fun toShort(value: TypeNameFormatPigeon): Short {
  return when (value) {
    TypeNameFormatPigeon.EMPTY -> NdefRecord.TNF_EMPTY
    TypeNameFormatPigeon.WELL_KNOWN -> NdefRecord.TNF_WELL_KNOWN
    TypeNameFormatPigeon.MEDIA -> NdefRecord.TNF_MIME_MEDIA
    TypeNameFormatPigeon.ABSOLUTE_URI -> NdefRecord.TNF_ABSOLUTE_URI
    TypeNameFormatPigeon.EXTERNAL -> NdefRecord.TNF_EXTERNAL_TYPE
    TypeNameFormatPigeon.UNKNOWN -> NdefRecord.TNF_UNKNOWN
    TypeNameFormatPigeon.UNCHANGED -> NdefRecord.TNF_UNCHANGED
  }
}

private fun toAdapterStatePigeon(value: Int): AdapterStatePigeon {
  return when (value) {
    NfcAdapter.STATE_OFF -> AdapterStatePigeon.OFF
    NfcAdapter.STATE_TURNING_ON -> AdapterStatePigeon.TURNING_ON
    NfcAdapter.STATE_ON -> AdapterStatePigeon.ON
    NfcAdapter.STATE_TURNING_OFF -> AdapterStatePigeon.TURNING_OFF
    else -> error("Unknown value: $value")
  }
}

private fun toTypeNameFormatPigeon(value: Short): TypeNameFormatPigeon {
  return when (value) {
    NdefRecord.TNF_EMPTY -> TypeNameFormatPigeon.EMPTY
    NdefRecord.TNF_WELL_KNOWN -> TypeNameFormatPigeon.WELL_KNOWN
    NdefRecord.TNF_MIME_MEDIA -> TypeNameFormatPigeon.MEDIA
    NdefRecord.TNF_ABSOLUTE_URI -> TypeNameFormatPigeon.ABSOLUTE_URI
    NdefRecord.TNF_EXTERNAL_TYPE -> TypeNameFormatPigeon.EXTERNAL
    NdefRecord.TNF_UNKNOWN -> TypeNameFormatPigeon.UNKNOWN
    NdefRecord.TNF_UNCHANGED -> TypeNameFormatPigeon.UNCHANGED
    else -> error("Unknown value: $value")
  }
}

private fun toNfcBarcodeTypePigeon(value: Int): NfcBarcodeTypePigeon {
  return when (value) {
    NfcBarcode.TYPE_KOVIO -> NfcBarcodeTypePigeon.KOVIO
    NfcBarcode.TYPE_UNKNOWN -> NfcBarcodeTypePigeon.UNKNOWN
    else -> error("Unknown value: $value")
  }
}

private fun toMifareClassicTypePigeon(value: Int): MifareClassicTypePigeon {
  return when (value) {
    MifareClassic.TYPE_CLASSIC -> MifareClassicTypePigeon.CLASSIC
    MifareClassic.TYPE_PLUS -> MifareClassicTypePigeon.PLUS
    MifareClassic.TYPE_PRO -> MifareClassicTypePigeon.PRO
    MifareClassic.TYPE_UNKNOWN -> MifareClassicTypePigeon.UNKNOWN
    else -> error("Unknown value: $value")
  }
}

private fun toMifareUltralightTypePigeon(value: Int): MifareUltralightTypePigeon {
  return when (value) {
    MifareUltralight.TYPE_ULTRALIGHT -> MifareUltralightTypePigeon.ULTRALIGHT
    MifareUltralight.TYPE_ULTRALIGHT_C -> MifareUltralightTypePigeon.ULTRALIGHT_C
    MifareUltralight.TYPE_UNKNOWN -> MifareUltralightTypePigeon.UNKNOWN
    else -> error("Unknown value: $value")
  }
}
