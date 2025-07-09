import 'dart:typed_data';

import 'package:nfc_manager/src/nfc_manager/nfc_manager.dart';
import 'package:nfc_manager/src/nfc_manager_android/pigeon.dart';
import 'package:nfc_manager/src/nfc_manager_android/pigeon.g.dart';
import 'package:nfc_manager/src/nfc_manager_android/tags/tag.dart';

/// The class providing access to NFC-B (ISO 14443-3B) operations for Android.
///
/// Acquire an instance using [from(NfcTag)].
final class NfcBAndroid {
  const NfcBAndroid._(
    this._handle, {
    required this.tag,
    required this.applicationData,
    required this.protocolInfo,
  });

  final String _handle;

  /// The tag object backing of this instance.
  final NfcTagAndroid tag;

  // TODO: DOC
  final Uint8List applicationData;

  // TODO: DOC
  final Uint8List protocolInfo;

  /// Creates an instance of this class for the given tag.
  ///
  /// Returns null if the tag is not compatible.
  static NfcBAndroid? from(NfcTag tag) {
    // ignore: invalid_use_of_protected_member
    final rawData = tag.data;
    final data = rawData is Map<Object?, Object?> 
        ? Map<String, dynamic>.from(rawData) 
        : rawData as Map<String, dynamic>?;
    final tech = data?['nfcb'] is Map<Object?, Object?>
        ? Map<String, dynamic>.from(data!['nfcb'] as Map<Object?, Object?>)
        : data?['nfcb'] as Map<String, dynamic>?;
    final atag = NfcTagAndroid.from(tag);
    if (data == null || tech == null || atag == null) return null;
    return NfcBAndroid._(
      data['handle'] as String,
      tag: atag,
      applicationData: tech['applicationData'] as Uint8List,
      protocolInfo: tech['protocolInfo'] as Uint8List,
    );
  }

  // TODO: DOC
  Future<int> getMaxTransceiveLength() {
    return hostApi.nfcBGetMaxTransceiveLength(handle: _handle);
  }

  // TODO: DOC
  Future<Uint8List> transceive(Uint8List bytes) {
    return hostApi.nfcBTransceive(handle: _handle, bytes: bytes);
  }
}
