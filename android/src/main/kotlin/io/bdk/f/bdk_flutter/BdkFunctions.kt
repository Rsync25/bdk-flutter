package io.bdk.f.bdk_flutter

import android.util.Log
import org.bitcoindevkit.*
import java.io.FileDescriptor
import org.bitcoindevkit.Wallet as BdkWallet

object BdkFunctions {
    private lateinit var wallet: BdkWallet
    const val TAG = "BDK-F"
    private val databaseConfig = DatabaseConfig.Memory
    private val blockchainConfig =
            BlockchainConfig.Electrum(
                    ElectrumConfig("ssl://electrum.blockstream.info:60002", null, 5u, null, 10u)
            )
    private var nodeNetwork = Network.TESTNET

    object ProgressLog : BdkProgress {
        override fun update(progress: Float, message: String?) {
            Log.i(progress.toString(), "Progress Log")
        }
    }

    //Init wallet
    init {
        initWallet()
        sync()
    }

    // Default wallet for initialization, which must be replaced with custom wallet for personal
    // use
    private fun initWallet(): BdkWallet {
        val key: ExtendedKeyInfo = seed(false, "default mnemonic", "password")
        createRestoreWallet(key, null, "", "", "", "", "", null)
        return this.wallet
    }

    private fun createRestoreWallet(
            keys: ExtendedKeyInfo, network: String?,
            blockChainConfigUrl: String, blockChainSocket5: String?,
            retry: String?, timeOut: String?, blockChain: String?, walletDescriptor: String?
    ) {
        try {
            val descriptor: String = walletDescriptor ?:  createDefaultDescriptor(keys)
            val changeDescriptor: String = createChangeDescriptorFromDescriptor(descriptor)
            val config: BlockchainConfig = createDatabaseConfig(
                    blockChainConfigUrl,
                    blockChainSocket5,
                    retry,
                    timeOut,
                    blockChain
            )
            this.wallet = BdkWallet(
                    descriptor,
                    changeDescriptor,
                    setNetwork(network),
                    databaseConfig,
                    config
            )
            sync()
        } catch (error: Error) {
            throw error
        }
    }

    fun createWallet(
            mnemonic: String, password: String?, network: String?,
            blockChainConfigUrl: String, blockChainSocket5: String?,
            retry: String?, timeOut: String?, blockChain: String?, walletDescriptor: String?
    ): Map<String, Any?> {
        try {
            val keys: ExtendedKeyInfo = seed(false, mnemonic, password)
            createRestoreWallet(
                    keys, network, blockChainConfigUrl, blockChainSocket5, retry,
                    timeOut, blockChain, walletDescriptor
            )
            val responseObject = mutableMapOf<String, Any?>()
            responseObject["address"] = wallet.getNewAddress()
            responseObject["mnemonic"] = keys.mnemonic
            responseObject["balance"] = wallet.getBalance().toString()
            Log.i(responseObject.toString(), "Progress Log Create Success")
            return responseObject
        } catch (error: Throwable) {
            throw(error)
        }
    }

    fun restoreWallet(
            mnemonic: String, password: String?, network: String?,
            blockChainConfigUrl: String, blockChainSocket5: String?,
            retry: String?, timeOut: String?, blockChain: String?,
            walletDescriptor: String?
    ): Map<String, Any?> {
        try {
            ///TODO Instead of making keys from password, passin Descriptor generated by keys
            val keys: ExtendedKeyInfo = seed(true, mnemonic, password)
            createRestoreWallet(
                    keys, network, blockChainConfigUrl, blockChainSocket5, retry,
                    timeOut, blockChain, walletDescriptor
            )
            val responseObject = mutableMapOf<String, Any?>()
            responseObject["address"] = wallet.getNewAddress()
            responseObject["balance"] = wallet.getBalance().toString()
            Log.i(responseObject.toString(), "Progress Log Restore Success")
            return responseObject
        } catch (error: Throwable) {
            throw(error)
        }
    }
    // please remove this
    fun getWallet(): String {
        try {
            return this.wallet.toString()
        } catch (error: Throwable) {
            throw(error)
        }
    }


    fun getNewAddress(): String {
        try {
            Log.i(wallet.getNewAddress(), "Progress Log Address")
            return this.wallet.getNewAddress()
        } catch (error: Throwable) {
            throw(error)
        }
    }

    fun getBalance(): String {
        try {
            Log.i(wallet.getBalance().toString(), "Progress Log Balance")
            return this.wallet.getBalance().toString()
        } catch (error: Throwable) {
            throw(error)
        }
    }

    fun createPartiallySignedBitcoinTransaction(recipient: String, amount: Double) :String{
        try {
            val longAmt: Long = amount.toLong()
            val psbt = PartiallySignedBitcoinTransaction(wallet, recipient, longAmt.toULong(), null)
            this.wallet.sign(psbt)
            Log.i(TAG, "psbt transaction ${psbt.serialize()}")
            return psbt.serialize();
        } catch (error: Throwable) {
            throw(error)
        }
    }
//    fun signAndBroadcastTransaction(recipient: String, amount: Double) :String{
//        try {
//            this.wallet.sign(psbt)
//            Log.i(TAG, "successfully broadcast the transaction")
//            val transaction: String = wallet.broadcast(psbt)
//            return (transaction)
//        } catch (error: Throwable) {
//            throw(error)
//        }
//
//    }
    //Create a 1-to-1 Transaction

    fun broadcastTx(recipient: String, amount: Double): String {
        try {
            val longAmt: Long = amount.toLong()
            val psbt = PartiallySignedBitcoinTransaction(wallet, recipient, longAmt.toULong(), null)
            wallet.sign(psbt)
            Log.i(TAG, "successfully broadcast $longAmt")
            val transaction: String = wallet.broadcast(psbt)
            return (transaction)
        } catch (error: Throwable) {
            throw(error)
        }
    }
    // retrieve transactions for an address
    fun pendingTransactionsList(): List<Map<String, Any?>> {
        try {
            val transactions =
                    this.wallet.getTransactions().filterIsInstance<Transaction.Unconfirmed>()
            if (transactions.isEmpty()) {
                Log.i(Bdk.TAG, "Pending transaction list is empty")
                return emptyList()
            } else {
                val unconfirmedTransactions: MutableList<Map<String, Any?>> = mutableListOf()
                for (item in transactions) {
                    val responseObject = mutableMapOf<String, Any?>()
                    responseObject["received"] = item.details.received.toString()
                    responseObject["sent"] = item.details.sent.toString()
                    responseObject["fees"] = item.details.fees.toString()
                    responseObject["txid"] = item.details.txid
                    unconfirmedTransactions.add(responseObject)
                }

                return unconfirmedTransactions
            }
        } catch (error: Throwable) {
            throw(error)
        }
    }

    fun confirmedTransactionsList(): List<Map<String, Any?>> {
        try {
            val transactions = wallet.getTransactions().filterIsInstance<Transaction.Confirmed>()
            if (transactions.isEmpty()) {
                Log.i(Bdk.TAG, "Confirmed transaction list is empty")
                return emptyList()
            } else {
                val confirmedTransactions: MutableList<Map<String, Any?>> = mutableListOf()
                for (item in transactions) {
                    val responseObject = mutableMapOf<String, Any?>()
                    responseObject["received"] = item.details.received.toString()
                    responseObject["sent"] = item.details.sent.toString()
                    responseObject["fees"] = item.details.fees.toString()
                    responseObject["txid"] = item.details.txid
                    responseObject["confirmation_time"] = item.confirmation.timestamp.toString()
                    confirmedTransactions.add(responseObject)
                }
                return confirmedTransactions
            }
        } catch (error: Throwable) {
            throw(error)
        }
    }

    fun resetWallet(): Boolean {
        try {
            wallet.destroy()
            Log.i(wallet.toString(), "Progress Log resetWallet Success")
            return true

        } catch (error: Throwable) {
            throw(error)
        }
    }

    fun  getLastUnusedAddress(): String{
        try {
            Log.i(wallet.getLastUnusedAddress(), "Progress Log Last Unused Addresss")
            return this.wallet.getBalance().toString()
        } catch (error: Throwable) {
            throw(error)
        }
    }
    // Bitcoin js functions

    //Generate a SegWit address descriptor
    private fun createDefaultDescriptor(keys: ExtendedKeyInfo): String {
        return "wpkh(" + keys.xprv + "/84'/1'/0'/0/*)"
    }
    // Generate a SegWit P2SH address descriptor
    private fun createP2SHP2WPKHDescriptor(mnemonic: String = "", password: String? = null): String {
        val keys: ExtendedKeyInfo = seed(true, mnemonic, password)
        return "sh(wpkh(" + keys.xprv + "/84'/1'/0'/0/*))"
    }
    //Generate a Static P2PKH descriptor
    private fun createP2PKHDescriptor(mnemonic: String = "", password: String? = null): String {
        val keys: ExtendedKeyInfo = seed(true, mnemonic, password)
        return "pkh(" + keys.xprv + "/84'/1'/0'/0/*)"
    }
    //Generate a SegWit 2-of-2 P2SH multisig address descriptor
    private fun createP2SH2of2MultisigDescriptor(mnemonic: String = "", password: String? = null, recipientPublicKey:String): String {
        val keys: ExtendedKeyInfo = seed(true, mnemonic, password)
        return "sh(multi(2" + keys.xprv +"," +recipientPublicKey+ "/84'/1'/0'/0/*))"
    }

    //Generate a SegWit 3-of-4 multisig address descriptor
    private fun createP2SH3of4MultisigDescriptor(mnemonic: String = "", password: String? = null, recipientPublicKey1:String, recipientPublicKey2:String, recipientPublicKey3:String): String {
        val keys: ExtendedKeyInfo = seed(true, mnemonic, password)
        return "sh(multi(2" + keys.xprv +"," +recipientPublicKey1+ "," + recipientPublicKey2 +","  +recipientPublicKey3+"/84'/1'/0'/0/*))"
    }




    //    private fun createChangeDescriptor(keys: ExtendedKeyInfo): String {
//        return "wpkh(" + keys.xprv + "/84'/1'/0'/1/*)"
//    }
    private fun createChangeDescriptorFromDescriptor(descriptor: String):String{
        return descriptor.replace("/84'/1'/0'/0/*","/84'/1'/0'/1/*")
    }

    fun seed(
            recover: Boolean = false,
            mnemonic: String = "",
            password: String? = null
    ): ExtendedKeyInfo {
        return if (!recover) generateExtendedKey(
                nodeNetwork,
                WordCount.WORDS12,
                password
        ) else restoreExtendedKey(nodeNetwork, mnemonic, password)
    }

    fun sync(maxAddress: UInt? = null): Unit {
        Log.i("Wallet", "Wallet is syncing")
        this.wallet.sync(ProgressLog, maxAddress)
    }

    private fun createDatabaseConfig(
            blockChainConfigUrl: String, blockChainSocket5: String?,
            retry: String?, timeOut: String?, blockChain: String?
    ): BlockchainConfig {
        return when (blockChain) {
            "ELECTRUM" -> BlockchainConfig.Electrum(
                    ElectrumConfig(
                            blockChainConfigUrl, blockChainSocket5,
                            retry?.toUByte() ?: 5u, timeOut?.toUByte() ?: 5u,
                            10u
                    )
            )
            "ESPLORA" -> BlockchainConfig.Esplora(
                    EsploraConfig(
                            blockChainConfigUrl, blockChainSocket5,
                            retry?.toULong() ?: 5u, timeOut?.toULong() ?: 5u,
                            10u
                    )
            )
            else -> {
                return blockchainConfig
            }
        }

    }

    private fun setNetwork(networkStr: String?): Network {
        return when (networkStr) {
            "TESTNET" -> Network.TESTNET
            "BITCOIN" -> Network.BITCOIN
            "REGTEST" -> Network.REGTEST
            "SIGNET" -> Network.SIGNET
            else -> {
                Network.TESTNET
            }
        }
    }
}