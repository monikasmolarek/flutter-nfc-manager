import 'dart:typed_data';

import 'package:ndef_record/ndef_record.dart';
import 'package:nfc_manager/src/nfc_manager/nfc_manager.dart';
import 'package:nfc_manager/src/nfc_manager_android/pigeon.dart';
import 'package:nfc_manager/src/nfc_manager_android/pigeon.g.dart';
import 'package:nfc_manager/src/nfc_manager_android/tags/tag.dart';

/// The class providing access to NDEF operations for Android.
///
/// Acquire an instance using [from(NfcTag)].
final class NdefAndroid {
  const NdefAndroid._(
    this._handle, {
    required this.tag,
    required this.type,
    required this.maxSize,
    required this.canMakeReadOnly,
    required this.isWritable,
    required this.cachedNdefMessage,
  });

  final String _handle;

  /// The tag object backing of this instance.
  final NfcTagAndroid tag;

  // TODO: DOC
  final String type;

  // TODO: DOC
  final int maxSize;

  // TODO: DOC
  final bool canMakeReadOnly;

  // TODO: DOC
  final bool isWritable;

  // TODO: DOC
  final NdefMessage? cachedNdefMessage;

  /// Creates an instance of this class for the given tag.
  ///
  /// Returns null if the tag is not compatible.
  static NdefAndroid? from(NfcTag tag) {
    // ignore: invalid_use_of_protected_member
    final rawData = tag.data;
    final data = rawData is Map<Object?, Object?> 
        ? Map<String, dynamic>.from(rawData) 
        : rawData as Map<String, dynamic>?;
    final tech = data?['ndef'] is Map<Object?, Object?>
        ? Map<String, dynamic>.from(data!['ndef'] as Map<Object?, Object?>)
        : data?['ndef'] as Map<String, dynamic>?;
    final atag = NfcTagAndroid.from(tag);
    if (data == null || tech == null || atag == null) return null;
    return NdefAndroid._(
      data['handle'] as String,
      tag: atag,
      type: tech['type'] as String,
      maxSize: tech['maxSize'] as int,
      canMakeReadOnly: tech['canMakeReadOnly'] as bool,
      isWritable: tech['isWritable'] as bool,
      cachedNdefMessage:
          tech['cachedNdefMessage'] == null
              ? null
              : NdefMessage(
                records:
                    (tech['cachedNdefMessage']!['records'] as List)
                        .map(
                          (r) => NdefRecord(
                            typeNameFormat: TypeNameFormat.values[(r as Map)['tnf'] as int],
                            type: (r as Map)['type'] as Uint8List,
                            identifier: (r as Map)['id'] as Uint8List,
                            payload: (r as Map)['payload'] as Uint8List,
                          ),
                        )
                        .toList(),
              ),
    );
  }

  // TODO: DOC
  Future<NdefMessage?> getNdefMessage() {
    return hostApi
        .ndefGetNdefMessage(handle: _handle)
        .then(
          (value) =>
              value == null
                  ? null
                  : NdefMessage(
                    records:
                        value.records
                            .map(
                              (r) => NdefRecord(
                                typeNameFormat: TypeNameFormat.values.byName(
                                  r.tnf.name,
                                ),
                                type: r.type,
                                identifier: r.id,
                                payload: r.payload,
                              ),
                            )
                            .toList(),
                  ),
        );
  }

  // TODO: DOC
  Future<void> writeNdefMessage(NdefMessage message) {
    return hostApi.ndefWriteNdefMessage(
      handle: _handle,
      message: NdefMessagePigeon(
        records:
            message.records
                .map(
                  (e) => NdefRecordPigeon(
                    tnf: TypeNameFormatPigeon.values.byName(
                      e.typeNameFormat.name,
                    ),
                    type: e.type,
                    id: e.identifier,
                    payload: e.payload,
                  ),
                )
                .toList(),
      ),
    );
  }

  // TODO: DOC
  Future<void> makeReadOnly() {
    return hostApi.ndefMakeReadOnly(handle: _handle);
  }
}
