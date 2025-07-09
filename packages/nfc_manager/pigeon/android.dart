import 'package:pigeon/pigeon.dart';

@ConfigurePigeon(
  PigeonOptions(
    dartOut: 'lib/src/nfc_manager_android/pigeon.g.dart',
    kotlinOut: 'android/src/main/kotlin/dev/flutter/plugins/nfcmanager/Pigeon.kt',
    kotlinOptions: KotlinOptions(package: 'dev.flutter.plugins.nfcmanager'),
  ),
)

@FlutterApi()
abstract final class FlutterApiPigeon {
  void onTagDiscovered(Map<String, Object> tag);
  void onAdapterStateChanged(AdapterStatePigeon state);
}

@HostApi()
abstract final class HostApiPigeon {
  bool nfcAdapterIsEnabled();
  bool nfcAdapterIsSecureNfcEnabled();
  bool nfcAdapterIsSecureNfcSupported();
  void nfcAdapterEnableReaderMode({required List<ReaderFlagPigeon> flags});
  void nfcAdapterDisableReaderMode();
  NdefMessagePigeon? ndefGetNdefMessage({required String handle});
  void ndefWriteNdefMessage({required String handle, required NdefMessagePigeon message});
  bool ndefMakeReadOnly({required String handle});
  int nfcAGetMaxTransceiveLength({required String handle});
  int nfcAGetTimeout({required String handle});
  void nfcASetTimeout({required String handle, required int timeout});
  Uint8List nfcATransceive({required String handle, required Uint8List bytes});
  int nfcBGetMaxTransceiveLength({required String handle});
  Uint8List nfcBTransceive({required String handle, required Uint8List bytes});
  int nfcFGetMaxTransceiveLength({required String handle});
  int nfcFGetTimeout({required String handle});
  void nfcFSetTimeout({required String handle, required int timeout});
  Uint8List nfcFTransceive({required String handle, required Uint8List bytes});
  int nfcVGetMaxTransceiveLength({required String handle});
  Uint8List nfcVTransceive({required String handle, required Uint8List bytes});
  int isoDepGetMaxTransceiveLength({required String handle});
  int isoDepGetTimeout({required String handle});
  void isoDepSetTimeout({required String handle, required int timeout});
  Uint8List isoDepTransceive({required String handle, required Uint8List bytes});
  int mifareClassicGetMaxTransceiveLength({required String handle});
  int mifareClassicGetTimeout({required String handle});
  void mifareClassicSetTimeout({required String handle, required int timeout});
  int mifareClassicBlockToSector({required String handle, required int blockIndex});
  int mifareClassicGetBlockCountInSector({required String handle, required int sectorIndex});
  int mifareClassicSectorToBlock({required String handle, required int sectorIndex});
  bool mifareClassicAuthenticateSectorWithKeyA({required String handle, required int sectorIndex, required Uint8List key});
  bool mifareClassicAuthenticateSectorWithKeyB({required String handle, required int sectorIndex, required Uint8List key});
  void mifareClassicIncrement({required String handle, required int blockIndex, required int value});
  void mifareClassicDecrement({required String handle, required int blockIndex, required int value});
  Uint8List mifareClassicReadBlock({required String handle, required int blockIndex});
  void mifareClassicWriteBlock({required String handle, required int blockIndex, required Uint8List data});
  void mifareClassicRestore({required String handle, required int blockIndex});
  void mifareClassicTransfer({required String handle, required int blockIndex});
  Uint8List mifareClassicTransceive({required String handle, required Uint8List bytes});
  int mifareUltralightGetMaxTransceiveLength({required String handle});
  int mifareUltralightGetTimeout({required String handle});
  void mifareUltralightSetTimeout({required String handle, required int timeout});
  Uint8List mifareUltralightReadPages({required String handle, required int pageOffset});
  void mifareUltralightWritePage({required String handle, required int pageOffset, required Uint8List data});
  Uint8List mifareUltralightTransceive({required String handle, required Uint8List bytes});
  void ndefFormatableFormat({required String handle, required NdefMessagePigeon firstMessage});
  void ndefFormatableFormatReadOnly({required String handle, required NdefMessagePigeon firstMessage});
}

final class NdefMessagePigeon {
  const NdefMessagePigeon({
    required this.records,
  });
  final List<NdefRecordPigeon> records;
}

final class NdefRecordPigeon {
  const NdefRecordPigeon({
    required this.tnf,
    required this.type,
    required this.id,
    required this.payload,
  });
  final TypeNameFormatPigeon tnf;
  final Uint8List type;
  final Uint8List id;
  final Uint8List payload;
}

enum ReaderFlagPigeon {
  nfcA,
  nfcB,
  nfcBarcode,
  nfcF,
  nfcV,
  noPlatformSounds,
  skipNdefCheck,
}

enum AdapterStatePigeon {
  off,
  turningOn,
  on,
  turningOff,
}

enum TypeNameFormatPigeon {
  empty,
  wellKnown,
  media,
  absoluteUri,
  external,
  unknown,
  unchanged,
}

enum NfcBarcodeTypePigeon {
  kovio,
  unknown,
}

enum MifareClassicTypePigeon {
  classic,
  plus,
  pro,
  unknown,
}

enum MifareUltralightTypePigeon {
  ultralight,
  ultralightC,
  unknown,
}
