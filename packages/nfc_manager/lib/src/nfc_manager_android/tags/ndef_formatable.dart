import 'package:ndef_record/ndef_record.dart';
import 'package:nfc_manager/src/nfc_manager/nfc_manager.dart';
import 'package:nfc_manager/src/nfc_manager_android/pigeon.dart';
import 'package:nfc_manager/src/nfc_manager_android/pigeon.g.dart';
import 'package:nfc_manager/src/nfc_manager_android/tags/tag.dart';

/// The class providing access to NDEF format operations for Android.
///
/// Acquire an instance using [from(NfcTag)].
final class NdefFormatableAndroid {
  const NdefFormatableAndroid._(this._handle, {required this.tag});

  final String _handle;

  /// The tag object backing of this instance.
  final NfcTagAndroid tag;

  /// Creates an instance of this class for the given tag.
  ///
  /// Returns null if the tag is not compatible.
  static NdefFormatableAndroid? from(NfcTag tag) {
    // ignore: invalid_use_of_protected_member
    final rawData = tag.data;
    final data = rawData is Map<Object?, Object?> 
        ? Map<String, dynamic>.from(rawData) 
        : rawData as Map<String, dynamic>?;
    final tech = data?['ndefFormatable'] as String?;
    final atag = NfcTagAndroid.from(tag);
    if (data == null || tech == null || atag == null) return null;
    return NdefFormatableAndroid._(data['handle'] as String, tag: atag);
  }

  // TODO: DOC
  Future<void> format(NdefMessage firstMessage) {
    return hostApi.ndefFormatableFormat(
      handle: _handle,
      firstMessage: NdefMessagePigeon(
        records:
            firstMessage.records
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
  Future<void> formatReadOnly(NdefMessage firstMessage) {
    return hostApi.ndefFormatableFormatReadOnly(
      handle: _handle,
      firstMessage: NdefMessagePigeon(
        records:
            firstMessage.records
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
}
