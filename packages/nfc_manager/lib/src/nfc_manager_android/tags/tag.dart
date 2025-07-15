import 'dart:typed_data';

import 'package:nfc_manager/src/nfc_manager/nfc_manager.dart';

/// The class providing access to tag operations for Android.
///
/// Acquire an instance using [from(NfcTag)].
final class NfcTagAndroid {
  const NfcTagAndroid._({required this.id, required this.techList});

  /// Creates an instance of this class for the given tag.
  ///
  /// Returns null if the tag is not compatible.
  static NfcTagAndroid? from(NfcTag tag) {
    // ignore: invalid_use_of_protected_member
    final rawData = tag.data;
    final data = rawData is Map<Object?, Object?> 
        ? Map<String, dynamic>.from(rawData) 
        : rawData as Map<String, dynamic>?;
    if (data == null) return null;
    return NfcTagAndroid._(id: data['id'] as Uint8List, techList: (data['techList'] as List).cast<String>());
  }

  // TODO: DOC
  final Uint8List id;

  // TODO: DOC
  final List<String> techList;
}
