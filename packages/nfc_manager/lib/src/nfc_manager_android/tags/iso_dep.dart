import 'dart:typed_data';

import 'package:nfc_manager/src/nfc_manager/nfc_manager.dart';
import 'package:nfc_manager/src/nfc_manager_android/pigeon.dart';
import 'package:nfc_manager/src/nfc_manager_android/pigeon.g.dart';
import 'package:nfc_manager/src/nfc_manager_android/tags/tag.dart';

/// The class providing access to ISO-DEP (ISO 14443-4) operations for Android.
///
/// Acquire an instance using [from(NfcTag)].
final class IsoDepAndroid {
  const IsoDepAndroid._(
    this._handle, {
    required this.tag,
    required this.hiLayerResponse,
    required this.historicalBytes,
    required this.isExtendedLengthApduSupported,
  });

  final String _handle;

  /// The tag object backing of this instance.
  final NfcTagAndroid tag;

  // TODO: DOC
  final Uint8List? hiLayerResponse;

  // TODO: DOC
  final Uint8List? historicalBytes;

  // TODO: DOC
  final bool isExtendedLengthApduSupported;

  /// Creates an instance of this class for the given tag.
  ///
  /// Returns null if the tag is not compatible.
  static IsoDepAndroid? from(NfcTag tag) {
    // ignore: invalid_use_of_protected_member
    final rawData = tag.data;
    final data = rawData is Map<Object?, Object?> 
        ? Map<String, dynamic>.from(rawData) 
        : rawData as Map<String, dynamic>?;
    final tech = data?['isodep'] is Map<Object?, Object?>
        ? Map<String, dynamic>.from(data!['isodep'] as Map<Object?, Object?>)
        : data?['isodep'] as Map<String, dynamic>?;
    final atag = NfcTagAndroid.from(tag);
    if (data == null || tech == null || atag == null) return null;
    return IsoDepAndroid._(
      data['handle'] as String,
      tag: atag,
      hiLayerResponse: tech['hiLayerResponse'] as Uint8List?,
      historicalBytes: tech['historicalBytes'] as Uint8List?,
      isExtendedLengthApduSupported: tech['isExtendedLengthApduSupported'] as bool,
    );
  }

  // TODO: DOC
  Future<int> getMaxTransceiveLength() {
    return hostApi.isoDepGetMaxTransceiveLength(handle: _handle);
  }

  // TODO: DOC
  Future<int> getTimeout() {
    return hostApi.isoDepGetTimeout(handle: _handle);
  }

  // TODO: DOC
  Future<void> setTimeout(int timeout) {
    return hostApi.isoDepSetTimeout(handle: _handle, timeout: timeout);
  }

  // TODO: DOC
  Future<Uint8List> transceive(Uint8List bytes) {
    return hostApi.isoDepTransceive(handle: _handle, bytes: bytes);
  }
}
