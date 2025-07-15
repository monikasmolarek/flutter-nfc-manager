import 'dart:typed_data';

import 'package:nfc_manager/src/nfc_manager/nfc_manager.dart';
import 'package:nfc_manager/src/nfc_manager_android/tags/tag.dart';

/// The class providing access to tags containing just a barcode for Android.
///
/// Acquire an instance using [from(NfcTag)].
final class NfcBarcodeAndroid {
  const NfcBarcodeAndroid._(
    this._handle, {
    required this.tag,
    required this.type,
    required this.barcode,
  });

  // ignore: unused_field
  final String _handle;

  /// The tag object backing of this instance.
  final NfcTagAndroid tag;

  // TODO: DOC
  final NfcBarcodeTypeAndroid type;

  // TODO: DOC
  final Uint8List barcode;

  /// Creates an instance of this class for the given tag.
  ///
  /// Returns null if the tag is not compatible.
  static NfcBarcodeAndroid? from(NfcTag tag) {
    // ignore: invalid_use_of_protected_member
    final rawData = tag.data;
    final data = rawData is Map<Object?, Object?> 
        ? Map<String, dynamic>.from(rawData) 
        : rawData as Map<String, dynamic>?;
    final tech = data?['nfcbarcode'] is Map<Object?, Object?>
        ? Map<String, dynamic>.from(data!['nfcbarcode'] as Map<Object?, Object?>)
        : data?['nfcbarcode'] as Map<String, dynamic>?;
    final atag = NfcTagAndroid.from(tag);
    if (data == null || tech == null || atag == null) return null;
    return NfcBarcodeAndroid._(
      data['handle'] as String,
      tag: atag,
      type: NfcBarcodeTypeAndroid.values[tech['type'] as int],
      barcode: tech['barcode'] as Uint8List,
    );
  }
}

// TODO: DOC
enum NfcBarcodeTypeAndroid {
  // TODO: DOC
  kovio,

  // TODO: DOC
  unknown,
}
