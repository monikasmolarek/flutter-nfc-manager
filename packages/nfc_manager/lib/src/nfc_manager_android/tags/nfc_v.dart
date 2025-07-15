import 'dart:typed_data';

import 'package:nfc_manager/src/nfc_manager/nfc_manager.dart';
import 'package:nfc_manager/src/nfc_manager_android/pigeon.dart';
import 'package:nfc_manager/src/nfc_manager_android/tags/tag.dart';

/// The class providing access to NFC-V (ISO 15693) operations for Android.
///
/// Acquire an instance using [from(NfcTag)].
final class NfcVAndroid {
  const NfcVAndroid._(
    this._handle, {
    required this.tag,
    required this.dsfId,
    required this.responseFlags,
  });

  final String _handle;

  /// The tag object backing of this instance.
  final NfcTagAndroid tag;

  // TODO: DOC
  final int dsfId;

  // TODO: DOC
  final int responseFlags;

  /// Creates an instance of this class for the given tag.
  ///
  /// Returns null if the tag is not compatible.
  static NfcVAndroid? from(NfcTag tag) {
    // ignore: invalid_use_of_protected_member
    final rawData = tag.data;
    final data = rawData is Map<Object?, Object?> 
        ? Map<String, dynamic>.from(rawData) 
        : rawData as Map<String, dynamic>?;
    final tech = data?['nfcv'] is Map<Object?, Object?>
        ? Map<String, dynamic>.from(data!['nfcv'] as Map<Object?, Object?>)
        : data?['nfcv'] as Map<String, dynamic>?;
    final atag = NfcTagAndroid.from(tag);
    if (data == null || tech == null || atag == null) return null;
    return NfcVAndroid._(
      data['handle'] as String,
      tag: atag,
      dsfId: tech['dsfId'] as int,
      responseFlags: tech['responseFlags'] as int,
    );
  }

  // TODO: DOC
  Future<int> getMaxTransceiveLength() {
    return hostApi.nfcVGetMaxTransceiveLength(handle: _handle);
  }

  // TODO: DOC
  Future<Uint8List> transceive(Uint8List bytes) {
    return hostApi.nfcVTransceive(handle: _handle, bytes: bytes);
  }
}
