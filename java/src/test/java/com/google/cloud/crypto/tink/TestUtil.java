// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.cloud.crypto.tink;

import static com.google.common.io.BaseEncoding.base16;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.crypto.tink.AesCtrHmacAeadProto.AesCtrHmacAeadKey;
import com.google.cloud.crypto.tink.AesCtrHmacAeadProto.AesCtrHmacAeadKeyFormat;
import com.google.cloud.crypto.tink.AesCtrProto.AesCtrKey;
import com.google.cloud.crypto.tink.AesCtrProto.AesCtrKeyFormat;
import com.google.cloud.crypto.tink.AesCtrProto.AesCtrParams;
import com.google.cloud.crypto.tink.AesGcmProto.AesGcmKey;
import com.google.cloud.crypto.tink.AesGcmProto.AesGcmKeyFormat;
import com.google.cloud.crypto.tink.CommonProto.EcPointFormat;
import com.google.cloud.crypto.tink.CommonProto.EllipticCurveType;
import com.google.cloud.crypto.tink.CommonProto.HashType;
import com.google.cloud.crypto.tink.EcdsaProto.EcdsaParams;
import com.google.cloud.crypto.tink.EcdsaProto.EcdsaPrivateKey;
import com.google.cloud.crypto.tink.EcdsaProto.EcdsaPublicKey;
import com.google.cloud.crypto.tink.EcdsaProto.EcdsaSignatureEncoding;
import com.google.cloud.crypto.tink.EciesAeadHkdfProto.EciesAeadDemParams;
import com.google.cloud.crypto.tink.EciesAeadHkdfProto.EciesAeadHkdfParams;
import com.google.cloud.crypto.tink.EciesAeadHkdfProto.EciesAeadHkdfPrivateKey;
import com.google.cloud.crypto.tink.EciesAeadHkdfProto.EciesAeadHkdfPublicKey;
import com.google.cloud.crypto.tink.EciesAeadHkdfProto.EciesHkdfKemParams;
import com.google.cloud.crypto.tink.GoogleCloudKmsProto.GoogleCloudKmsAeadKey;
import com.google.cloud.crypto.tink.HmacProto.HmacKey;
import com.google.cloud.crypto.tink.HmacProto.HmacKeyFormat;
import com.google.cloud.crypto.tink.HmacProto.HmacParams;
import com.google.cloud.crypto.tink.KmsEnvelopeProto.KmsEnvelopeAeadKey;
import com.google.cloud.crypto.tink.KmsEnvelopeProto.KmsEnvelopeAeadParams;
import com.google.cloud.crypto.tink.TinkProto.KeyFormat;
import com.google.cloud.crypto.tink.TinkProto.KeyStatusType;
import com.google.cloud.crypto.tink.TinkProto.Keyset;
import com.google.cloud.crypto.tink.TinkProto.Keyset.Key;
import com.google.cloud.crypto.tink.TinkProto.OutputPrefixType;
import com.google.cloud.crypto.tink.subtle.EcUtil;
import com.google.cloud.crypto.tink.subtle.Random;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Test helpers.
 */
@RunWith(JUnit4.class)
public class TestUtil {
  /**
   * A dummy Mac for testing.
   */
  public static class DummyMac implements Mac {
    public DummyMac() {}
    @Override
    public byte[] computeMac(byte[] data) throws GeneralSecurityException {
      return data;
    }
    @Override
    public void verifyMac(byte[] mac, byte[] data) throws GeneralSecurityException {
      return;
    }
  }

  /**
   * A key manager for DummyMac keys.
   */
  public static class DummyMacKeyManager implements KeyManager<Mac, Message, Message> {
    public DummyMacKeyManager() {}

    @Override
    public Mac getPrimitive(byte[] serialized) throws GeneralSecurityException {
      return new DummyMac();
    }
    @Override
    public Mac getPrimitive(Message proto) throws GeneralSecurityException {
      return new DummyMac();
    }
    @Override
    public Message newKey(byte[] serialized) throws GeneralSecurityException {
      return Any.newBuilder().setTypeUrl(this.getClass().getSimpleName()).build();
    }
    @Override
    public Message newKey(Message format) throws GeneralSecurityException {
      return Any.newBuilder().setTypeUrl(this.getClass().getSimpleName()).build();
    }
    @Override
    public boolean doesSupport(String typeUrl) {
      return typeUrl.equals(this.getClass().getSimpleName());
    }
  }

  /**
   * A dummy Aead-implementation that just returns the plaintext.
   */
  public static class EchoAead implements Aead {
    public EchoAead() {}

    @Override
    public byte[] encrypt(byte[] plaintext, byte[] aad) throws GeneralSecurityException {
      return plaintext;
    }
    @Override
    public byte[] decrypt(byte[] ciphertext, byte[] aad) throws GeneralSecurityException {
      return ciphertext;
    }
    @Override
    public Future<byte[]> asyncEncrypt(byte[] plaintext, byte[] aad) {
      return null;
    }
    @Override
    public Future<byte[]> asyncDecrypt(byte[] ciphertext, byte[] aad) {
      return null;
    }
  }

  /**
   * A key manager for EchoAead keys that just echo plaintext.
   */
  public static class EchoAeadKeyManager implements KeyManager<Aead, Message, Message> {
    public EchoAeadKeyManager() {}

    @Override
    public Aead getPrimitive(byte[] serialized) throws GeneralSecurityException {
      return new EchoAead();
    }
    @Override
    public Aead getPrimitive(Message proto) throws GeneralSecurityException {
      return new EchoAead();
    }
    @Override
    public Message newKey(byte[] serialized) throws GeneralSecurityException {
      return Any.newBuilder().setTypeUrl(this.getClass().getSimpleName()).build();
    }
    @Override
    public Message newKey(Message format) throws GeneralSecurityException {
      return Any.newBuilder().setTypeUrl(this.getClass().getSimpleName()).build();
    }
    @Override
    public boolean doesSupport(String typeUrl) {
      return typeUrl.equals(this.getClass().getSimpleName());
    }
  }

  /**
   * A dummy Aead-Implementation that returns empty byte arrays.
   */
  public static class FaultyAead implements Aead {
    public FaultyAead() {}

    @Override
    public byte[] encrypt(byte[] plaintext, byte[] aad) throws GeneralSecurityException {
      return new byte[0];
    }
    @Override
    public byte[] decrypt(byte[] ciphertext, byte[] aad) throws GeneralSecurityException {
      return new byte[0];
    }
    @Override
    public Future<byte[]> asyncEncrypt(byte[] plaintext, byte[] aad) {
      return null;
    }
    @Override
    public Future<byte[]> asyncDecrypt(byte[] ciphertext, byte[] aad) {
      return null;
    }
  }

  /**
   * A key manager for FaultyAead keys that always return an empty byte array on all encrypt or
   * decrypt requests.
   */
  public static class FaultyAeadKeyManager implements KeyManager<Aead, Message, Message> {
    public FaultyAeadKeyManager() {}

    @Override
    public Aead getPrimitive(byte[] serialized) throws GeneralSecurityException {
      return new FaultyAead();
    }
    @Override
    public Aead getPrimitive(Message proto) throws GeneralSecurityException {
      return new FaultyAead();
    }
    @Override
    public Message newKey(byte[] serialized) throws GeneralSecurityException {
      return Any.newBuilder().setTypeUrl(this.getClass().getSimpleName()).build();
    }
    @Override
    public Message newKey(Message format) throws GeneralSecurityException {
      return Any.newBuilder().setTypeUrl(this.getClass().getSimpleName()).build();
    }
    @Override
    public boolean doesSupport(String typeUrl) {
      return typeUrl.equals(this.getClass().getSimpleName());
    }
  }

  /**
   * @return a keyset from a list of keys. The first key is primary.
   */
  public static Keyset createKeyset(Key primary, Key... keys) throws Exception {
    Keyset.Builder builder = Keyset.newBuilder();
    builder.addKey(primary)
        .setPrimaryKeyId(primary.getKeyId());
    for (Key key : keys) {
      builder.addKey(key);
    }
    return builder.build();
  }

  /**
   * @return a key with some specific properties.
   */
  public static Key createKey(Message proto, int keyId, KeyStatusType status,
      OutputPrefixType prefixType) throws Exception {
    return Key.newBuilder()
        .setKeyData(Any.pack(proto))
        .setStatus(status)
        .setKeyId(keyId)
        .setOutputPrefixType(prefixType)
        .build();
  }

  /**
   * @return a {@code HmacKey}.
   */
  public static HmacKey createHmacKey(byte[] keyValue, int tagSize) throws Exception {
    HmacParams params = HmacParams.newBuilder()
        .setHash(HashType.SHA256)
        .setTagSize(tagSize)
        .build();

    return HmacKey.newBuilder()
        .setParams(params)
        .setKeyValue(ByteString.copyFrom(keyValue))
        .build();
  }

  /**
   * @return a {@code AesCtrKey}.
   */
  public static AesCtrKey createAesCtrKey(byte[] keyValue, int ivSize) throws Exception {
    AesCtrParams aesCtrParams = AesCtrParams.newBuilder()
        .setIvSize(ivSize)
        .build();
    return AesCtrKey.newBuilder()
        .setParams(aesCtrParams)
        .setKeyValue(ByteString.copyFrom(keyValue))
        .build();
  }

  /**
   * @return a {@code AesCtrHmacAeadKey}.
   */
  public static AesCtrHmacAeadKey createAesCtrHmacAeadKey(byte[] aesCtrKeyValue, int ivSize,
      byte[] hmacKeyValue, int tagSize) throws Exception {
    AesCtrKey aesCtrKey = createAesCtrKey(aesCtrKeyValue, ivSize);
    HmacKey hmacKey = createHmacKey(hmacKeyValue, tagSize);

    return AesCtrHmacAeadKey.newBuilder()
        .setAesCtrKey(aesCtrKey)
        .setHmacKey(hmacKey)
        .build();
  }

  /**
   * @return a {@code AesGcmKey}.
   */
  public static AesGcmKey createAesGcmKey(byte[] keyValue) throws Exception {
    return AesGcmKey.newBuilder()
        .setKeyValue(ByteString.copyFrom(keyValue))
        .build();
  }

  /**
   * @return a {@code AesCtrHmacAeadKeyFormat}.
   */
  public static KeyFormat createAesCtrHmacAeadKeyFormat(int aesKeySize, int ivSize,
      int hmacKeySize, int tagSize) throws Exception {
    AesCtrKeyFormat aesCtrKeyFormat = AesCtrKeyFormat.newBuilder()
        .setParams(AesCtrParams.newBuilder().setIvSize(ivSize).build())
        .setKeySize(aesKeySize)
        .build();
    HmacKeyFormat hmacKeyFormat = HmacKeyFormat.newBuilder()
        .setParams(
            HmacParams.newBuilder().setHash(HashType.SHA256).setTagSize(tagSize).build())
        .setKeySize(hmacKeySize)
        .build();
    AesCtrHmacAeadKeyFormat format = AesCtrHmacAeadKeyFormat.newBuilder()
        .setAesCtrKeyFormat(aesCtrKeyFormat)
        .setHmacKeyFormat(hmacKeyFormat)
        .build();
    return KeyFormat.newBuilder()
        .setFormat(Any.pack(format))
        .setKeyType("type.googleapis.com/google.cloud.crypto.tink.AesCtrHmacAeadKey")
        .build();
  }

  /**
   * @return a {@code AesGcmKeyFormat}.
   */
  public static KeyFormat createAesGcmKeyFormat(int keySize) throws Exception {
    AesGcmKeyFormat format = AesGcmKeyFormat.newBuilder()
        .setKeySize(keySize)
        .build();
    return KeyFormat.newBuilder()
        .setFormat(Any.pack(format))
        .setKeyType("type.googleapis.com/google.cloud.crypto.tink.AesGcmKey")
        .build();
  }

  /**
   * @return a {@code GoogleCloudKmsAeadKey}.
   */
  public static GoogleCloudKmsAeadKey createGoogleCloudKmsAeadKey(String kmsKeyUri)
      throws Exception {
    return GoogleCloudKmsAeadKey.newBuilder()
        .setKmsKeyUri(kmsKeyUri)
        .build();
  }

  /**
   * @return a {@code KmsEnvelopeAeadKey}.
   */
  public static KmsEnvelopeAeadKey createKmsEnvelopeAeadKey(Any kmsKey, KeyFormat dekFormat)
      throws Exception {
    KmsEnvelopeAeadParams params = KmsEnvelopeAeadParams.newBuilder()
        .setDekFormat(dekFormat)
        .setKmsKey(kmsKey)
        .build();
    return KmsEnvelopeAeadKey.newBuilder().setParams(params).build();
  }

  /**
   * @return a KMS key URI in a format defined by Google Cloud KMS.
   */
  public static String createGoogleCloudKmsKeyUri(
    String projectId, String location, String ringId, String keyId) {
    return String.format(
        "projects/%s/locations/%s/keyRings/%s/cryptoKeys/%s",
        projectId, location, ringId, keyId);
  }

  /**
   * @return a keyset handle from a {@code keyset}.
   */
  public static KeysetHandle createKeysetHandle(final Keyset keyset) throws Exception {
    return new KeysetHandle(keyset);
  }

  /**
   * @return a keyset handle from a {@code keyset} which must be a Keyset proto in text format.
   */
  public static KeysetHandle createKeysetHandle(final String keyset) throws Exception {
    try {
      Keyset.Builder keysetBuilder = Keyset.newBuilder();
      TextFormat.merge(keyset, keysetBuilder);
      return createKeysetHandle(keysetBuilder.build());
    } catch (Exception e) {
      System.out.println(e);
      return null;
    }
  }

  /**
   * Runs basic tests against an Aead primitive.
   */
  public static void runBasicTests(Aead aead) throws Exception {
    byte[] plaintext = Random.randBytes(20);
    byte[] associatedData = Random.randBytes(20);
    byte[] ciphertext = aead.encrypt(plaintext, associatedData);
    byte[] decrypted = aead.decrypt(ciphertext, associatedData);
    assertArrayEquals(plaintext, decrypted);

    byte original = ciphertext[0];
    ciphertext[0] = (byte) ~original;
    try {
      aead.decrypt(ciphertext, associatedData);
      fail("Expected GeneralSecurityException");
    } catch (GeneralSecurityException e) {
      assertTrue(e.toString().contains("decryption failed"));
    }
    ciphertext[0] = original;
    original = ciphertext[CryptoFormat.NON_RAW_PREFIX_SIZE];
    ciphertext[CryptoFormat.NON_RAW_PREFIX_SIZE] = (byte) ~original;
    try {
      aead.decrypt(ciphertext, associatedData);
      fail("Expected GeneralSecurityException");
    } catch (GeneralSecurityException e) {
      assertTrue(e.toString().contains("decryption failed"));
    }

    ciphertext[0] = original;
    original = associatedData[0];
    associatedData[0] = (byte) ~original;
    try {
      aead.decrypt(ciphertext, associatedData);
      fail("Expected GeneralSecurityException");
    } catch (GeneralSecurityException e) {
      assertTrue(e.toString().contains("decryption failed"));
    }

    // async tests
    ciphertext = aead.asyncEncrypt(plaintext, associatedData).get();
    decrypted = aead.asyncDecrypt(ciphertext, associatedData).get();
    assertArrayEquals(plaintext, decrypted);
    for (int length = 0; length < ciphertext.length; length++) {
      byte[] truncated = Arrays.copyOf(ciphertext, length);
      try {
        byte[] unused = aead.asyncDecrypt(truncated, associatedData).get();
        fail("Decrypting a truncated ciphertext should fail");
      } catch (ExecutionException ex) {
        // The decryption should fail because the ciphertext has been truncated.
        assertTrue(ex.getCause() instanceof GeneralSecurityException);
      }
    }
  }

  /**
   * @return a {@code EcdsaPrivateKey} constructed from {@code EcdsaPublicKey} and the byte array
   * of private key.
   */
  public static EcdsaPrivateKey createEcdsaPrivKey(EcdsaPublicKey pubKey, byte[] privKey) {
    final int version = 0;
    return EcdsaPrivateKey.newBuilder()
        .setVersion(version)
        .setPublicKey(pubKey)
        .setKeyValue(ByteString.copyFrom(privKey))
        .build();
  }

  /**
   * @return a {@code EcdsaPublicKey} constructed from {@code EllipticCurveType} and
   * {@code HashType}.
   */
  public static EcdsaPublicKey generateEcdsaPubKey(EllipticCurveType curve, HashType hashType,
      EcdsaSignatureEncoding encoding)
    throws Exception {
    EcdsaPrivateKey privKey = generateEcdsaPrivKey(curve, hashType, encoding);
    return privKey.getPublicKey();
  }

  /**
   * @return a {@code EcdsaPrivateKey} constructed from {@code EllipticCurveType} and
   * {@code HashType}.
   */
  public static EcdsaPrivateKey generateEcdsaPrivKey(EllipticCurveType curve, HashType hashType,
      EcdsaSignatureEncoding encoding)
      throws Exception {
        ECParameterSpec ecParams;
        switch(curve) {
          case NIST_P256:
            ecParams = EcUtil.getNistP256Params();
            break;
          case NIST_P384:
            ecParams = EcUtil.getNistP384Params();
            break;
          case NIST_P521:
            ecParams = EcUtil.getNistP521Params();
            break;
          default:
            throw new NoSuchAlgorithmException("Curve not implemented:" + curve);
        }
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(ecParams);
        KeyPair keyPair = keyGen.generateKeyPair();
        ECPublicKey pubKey = (ECPublicKey) keyPair.getPublic();
        ECPrivateKey privKey = (ECPrivateKey) keyPair.getPrivate();
        ECPoint w = pubKey.getW();
        EcdsaPublicKey ecdsaPubKey = createEcdsaPubKey(hashType, curve, encoding,
            w.getAffineX().toByteArray(), w.getAffineY().toByteArray());

        return createEcdsaPrivKey(ecdsaPubKey, privKey.getS().toByteArray());
      }

  /**
   * @return a {@code EcdsaPublicKey} constructed from {@code HashType}, {@code EllipticCurveType}
   * and affine coordinates of the public key.
   */
  public static EcdsaPublicKey createEcdsaPubKey(HashType hashType, EllipticCurveType curve,
      EcdsaSignatureEncoding encoding, byte[] pubX, byte[] pubY) throws Exception {
    final int version = 0;
    EcdsaParams ecdsaParams = EcdsaParams.newBuilder()
        .setHashType(hashType)
        .setCurve(curve)
        .setEncoding(encoding)
        .build();
    return EcdsaPublicKey.newBuilder()
        .setVersion(version)
        .setParams(ecdsaParams)
        .setX(ByteString.copyFrom(pubX))
        .setY(ByteString.copyFrom(pubY))
        .build();
  }

  /**
   * @return a freshly generated {@code EciesAeadHkdfPrivateKey} constructed with specified
   * parameters.
   */
  public static EciesAeadHkdfPrivateKey generateEciesAeadHkdfPrivKey(EllipticCurveType curve,
      HashType hashType, EcPointFormat pointFormat, KeyFormat demKeyFormat, byte[] salt)
      throws Exception {
    ECParameterSpec ecParams;
    switch(curve) {
      case NIST_P256:
        ecParams = EcUtil.getNistP256Params();
        break;
      case NIST_P384:
        ecParams = EcUtil.getNistP384Params();
        break;
      case NIST_P521:
        ecParams = EcUtil.getNistP521Params();
        break;
      default:
        throw new NoSuchAlgorithmException("Curve not implemented:" + curve);
    }
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
    keyGen.initialize(ecParams);
    KeyPair keyPair = keyGen.generateKeyPair();
    ECPublicKey pubKey = (ECPublicKey) keyPair.getPublic();
    ECPrivateKey privKey = (ECPrivateKey) keyPair.getPrivate();
    ECPoint w = pubKey.getW();
    EciesAeadHkdfPublicKey eciesPubKey = createEciesAeadHkdfPubKey(curve, hashType, pointFormat,
        demKeyFormat, w.getAffineX().toByteArray(), w.getAffineY().toByteArray(), salt);
    return createEciesAeadHkdfPrivKey(eciesPubKey, privKey.getS().toByteArray());
  }

  /**
   *  @return a {@code EciesAeadHkdfPrivateKey} with the specified key material and parameters.
   */
  public static EciesAeadHkdfPrivateKey createEciesAeadHkdfPrivKey(EciesAeadHkdfPublicKey pubKey,
      byte[] privKeyValue) throws Exception {
    final int version = 0;
    return EciesAeadHkdfPrivateKey.newBuilder()
        .setVersion(version)
        .setPublicKey(pubKey)
        .setKeyValue(ByteString.copyFrom(privKeyValue))
        .build();
  }

  /**
   *  @return a {@code EciesAeadHkdfParams} with the specified parameters.
   */
  public static EciesAeadHkdfParams createEciesAeadHkdfParams(EllipticCurveType curve,
      HashType hashType, EcPointFormat ecPointFormat, KeyFormat demKeyFormat,
      byte[] salt) throws Exception {
    EciesHkdfKemParams kemParams = EciesHkdfKemParams.newBuilder()
        .setCurveType(curve)
        .setHkdfHashType(hashType)
        .setHkdfSalt(ByteString.copyFrom(salt))
        .build();
    EciesAeadDemParams demParams = EciesAeadDemParams.newBuilder()
        .setAeadDem(demKeyFormat)
        .build();
    return EciesAeadHkdfParams.newBuilder()
        .setKemParams(kemParams)
        .setDemParams(demParams)
        .setEcPointFormat(ecPointFormat)
        .build();
  }

  /**
   *  @return a {@code EciesAeadHkdfPublicKey} with the specified key material and parameters.
   */
  public static EciesAeadHkdfPublicKey createEciesAeadHkdfPubKey(EllipticCurveType curve,
      HashType hashType, EcPointFormat ecPointFormat, KeyFormat demKeyFormat,
      byte[] pubX, byte[] pubY, byte[] salt) throws Exception {
    final int version = 0;
    EciesAeadHkdfParams params = createEciesAeadHkdfParams(curve, hashType, ecPointFormat,
        demKeyFormat, salt);
    return EciesAeadHkdfPublicKey.newBuilder()
        .setVersion(version)
        .setParams(params)
        .setX(ByteString.copyFrom(pubX))
        .setY(ByteString.copyFrom(pubY))
        .build();
  }

  /**
   * Decodes hex string.
   */
  public static byte[] hexDecode(String hexData) {
    return base16().lowerCase().decode(hexData);
  }

  /**
   * Encodes bytes to hex string.
   */
  public static String hexEncode(byte[] data) {
    return base16().lowerCase().encode(data);
  }
}
