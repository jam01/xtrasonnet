# xtr.crypto

[//]: # (todo: document available algorithms or point to the docs)

## decrypt
`decrypt(data: String, key: String, transformation: String): String`

Decrypts the Base64 `data` with specified Java Cryptographic `transformation` and the given `key`.

The `transformation` must include the name of a cryptographic algorithm (e.g., AES), and may be followed by a feedback mode and padding scheme. A transformation is of the form: 'algorithm/mode/padding' or 'algorithm'. See the [Java Cipher](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/javax/crypto/Cipher.html) docs for more information.

**Example**
```
xtr.crypto.decrypt('vAkb3PWJPQF1kGkM3vQrdQ==', '$sixteencharkey$', 'AES/ECB/PKCS5Padding')
```
**Result**
```
'Hello, world!'
```

!!! note
	xtrasonnet supports decryption through the Java Cryptographic Extension (JCE) framework. Every Java distribution is required to support a specific set of algorithms, while others may only be supported by third party libraries such as the popular [Bouncy Castle](https://www.bouncycastle.org/java.html) libraries, which are bundled with xtrasonnet.

!!! note
	In order to facilitate encryption/decryption, xtrasonnet prefixes the encrypted data with the randomly generated initialization vector (IV) used by the JCE Cipher. Similarly, decryption operations expect such IV to be present in order to correctly decrypt the data.  

    The form of the payload byte array expected for decryption is as follows: `[{encryption IV bytes},{encrypted data bytes}]` where the size of the IV portion is equal to the block size for the selected algorithm.

<br/>
## encrypt
`encrypt(data: String, key: String, transformation: String): String`

Encrypts the `data` with specified Java Cryptographic `transformation` and the given `key`, and encodes the result in Base64.

The `transformation` must include the name of a cryptographic algorithm (e.g., AES), and may be followed by a feedback mode and padding scheme. A transformation is of the form: 'algorithm/mode/padding' or 'algorithm'. See the [Java Cipher](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/javax/crypto/Cipher.html) docs for more information.

**Example**
```
xtr.crypto.encrypt('Hello, world!', '$sixteencharkey$', 'AES/ECB/PKCS5Padding')
```
**Result**
```
'vAkb3PWJPQF1kGkM3vQrdQ=='
```

!!! note
	xtrasonnet supports encryption through the Java Cryptographic Extension (JCE) framework. Every Java distribution is required to support a specific set of algorithms, while others may only be found in third party libraries such as the popular [Bouncy Castle](https://www.bouncycastle.org/java.html) libraries, which are bundled with xtrasonnet.

!!! note
	In order to facilitate encryption/decryption, xtrasonnet prefixes the encrypted data with the randomly generated initialization vector (IV) used by the JCE Cipher. Similarly decryption operations expect such IV to be present in order to correctly decrypt the data.  

    The form of the encrypted byte array payload is as follows: `[{random IV bytes},{encrypted data bytes}]` where the size of the IV portion is equal to the block size for the selected algorithm.

<br/>
## hash
`hash(data: String, algorithm: String): String`

Calculates Message Digest for the `data` using the given hash `algorithm`, and encodes the result in a hexadecimal string.

**Example**
```
xtr.crypto.hash('HelloWorld', 'MD5')
```
**Result**
```
'68e109f0f40ca72a15e05cc22786f8e6'
```

!!! note
	xtrasonnet supports Message Digests through the Java Security framework. Every Java distribution is required to support a specific set of algorithms, while others may only be supported by third party libraries. Users may be able to use an extended set of algorithms by adding any such library to the classpath.


<br/>
## hmac
`hmac(data: String, key: String, algorithm: String): String`

Calculates the Message Authentication Code for the `data` using the given cryptographic hash `algorithm`, and encodes the result in a hexadecimal string.

**Example**
```
xtr.crypto.hmac('HelloWorld', 'xtrasonnet rules!', 'HMACSHA256')
```
**Result**
```
'7854220ef827b07529509f68f391a80bf87fff328dbda140ed582520a1372dc1'
```

!!! note
	xtrasonnet supports HMAC through the Java Security framework. Every Java distribution is required to support a specific set of algorithms, while others may only be supported by third party libraries. Users may be able to use an extended set of algorithms by adding any such library to the classpath.
