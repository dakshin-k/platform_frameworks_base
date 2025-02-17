/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.telephony.ims;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.StringDef;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.telephony.ims.stub.SipDelegate;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The IMS registration and other attributes that the {@link SipDelegateConnection} used by the
 * IMS application will need to be aware of to correctly generate outgoing {@link SipMessage}s.
 * <p>
 * The IMS service must generate new instances of this configuration as the IMS configuration
 * managed by the IMS service changes. Along with each {@link SipDelegateImsConfiguration} instance
 * containing the configuration is the "version", which should be incremented every time a new
 * {@link SipDelegateImsConfiguration} instance is created. The {@link SipDelegateConnection} will
 * include the version of the {@link SipDelegateImsConfiguration} instance that it used in order for
 * the {@link SipDelegate} to easily identify if the IMS application used a now stale configuration
 * to generate the {@link SipMessage} and return
 * {@link SipDelegateManager#MESSAGE_FAILURE_REASON_STALE_IMS_CONFIGURATION} in
 * {@link DelegateMessageCallback#onMessageSendFailure(String, int)} so that the IMS application can
 * regenerate that {@link SipMessage} using the correct {@link SipDelegateImsConfiguration}
 * instance.
 * <p>
 * Every time the IMS configuration state changes in the IMS service, a full configuration should
 * be generated. The new  {@link SipDelegateImsConfiguration} instance should not be an incremental
 * update.
 * @hide
 */
@SystemApi
public final class SipDelegateImsConfiguration implements Parcelable {

    /**
     * IPV4 Address type.
     * <p>
     * Used as a potential value for {@link #KEY_SIP_CONFIG_IPTYPE_STRING}.
     */
    public static final String IPTYPE_IPV4 = "IPV4";

    /**
     * IPV6 Address type.
     * <p>
     * Used as a potential value for {@link #KEY_SIP_CONFIG_IPTYPE_STRING}.
     */
    public static final String IPTYPE_IPV6 = "IPV6";

    /**
     * The SIP transport uses UDP.
     * <p>
     * Used as a potential value for {@link #KEY_SIP_CONFIG_TRANSPORT_TYPE_STRING}.
     */
    public static final String SIP_TRANSPORT_UDP = "UDP";

    /**
     * The SIP transport uses TCP.
     * <p>
     * Used as a potential value for {@link #KEY_SIP_CONFIG_TRANSPORT_TYPE_STRING}.
     */
    public static final String SIP_TRANSPORT_TCP = "TCP";

    /**
     * Flag specifying if SIP compact form is enabled
     */
    public static final String KEY_SIP_CONFIG_IS_COMPACT_FORM_ENABLED_BOOL =
            "sip_config_is_compact_form_enabled_bool";

    /**
     * Flag specifying if SIP keepalives are enabled
     */
    public static final String KEY_SIP_CONFIG_IS_KEEPALIVE_ENABLED_BOOL =
            "sip_config_is_keepalive_enabled_bool";

    /**
     * Maximum SIP payload to be sent on UDP. If the SIP message payload is greater than max udp
     * payload size, then TCP must be used
     */
    public static final String KEY_SIP_CONFIG_MAX_PAYLOAD_SIZE_ON_UDP_INT =
            "sip_config_udp_max_payload_size_int";

    /**
     * Transport protocol used for SIP signaling.
     * Available options are: {@link #SIP_TRANSPORT_UDP }, {@link #SIP_TRANSPORT_TCP }
     */
    public static final String KEY_SIP_CONFIG_TRANSPORT_TYPE_STRING =
            "sip_config_protocol_type_string";

    /**
     * IMS public user identifier string
     */
    public static final String KEY_SIP_CONFIG_UE_PUBLIC_USER_ID_STRING =
            "sip_config_ue_public_user_id_string";

    /**
     * IMS private user identifier string
     */
    public static final String KEY_SIP_CONFIG_UE_PRIVATE_USER_ID_STRING =
            "sip_config_ue_private_user_id_string";

    /**
     * IMS home domain string
     */
    public static final String KEY_SIP_CONFIG_HOME_DOMAIN_STRING = "sip_config_home_domain_string";

    /**
     * IMEI string. Application can include the Instance-ID feature tag " +sip.instance" in the
     * Contact header with a value of the device IMEI in the form "urn:gsma:imei:<device IMEI>".
     */
    public static final String KEY_SIP_CONFIG_IMEI_STRING = "sip_config_imei_string";

    /**
     * IP address type for SIP signaling.
     * Available options are: {@link #IPTYPE_IPV6}, {@link #IPTYPE_IPV4}
     */
    public static final String KEY_SIP_CONFIG_IPTYPE_STRING = "sip_config_iptype_string";

    /**
     * Local IPaddress used for SIP signaling.
     */
    public static final String KEY_SIP_CONFIG_UE_DEFAULT_IPADDRESS_STRING =
            "sip_config_ue_default_ipaddress_string";

    /**
     * Local port used for sending SIP traffic
     */
    public static final String KEY_SIP_CONFIG_UE_DEFAULT_PORT_INT =
            "sip_config_ue_default_port_int";

    /**
     * SIP server / PCSCF default ip address
     */
    public static final String KEY_SIP_CONFIG_SERVER_DEFAULT_IPADDRESS_STRING =
            "sip_config_server_default_ipaddress_string";

    /**
     * SIP server / PCSCF port used for sending SIP traffic
     */
    public static final String KEY_SIP_CONFIG_SERVER_DEFAULT_PORT_INT =
            "sip_config_server_default_port_int";

    /**
     * Flag specifying if Network Address Translation is enabled and UE is behind a NAT.
     */
    public static final String KEY_SIP_CONFIG_IS_NAT_ENABLED_BOOL =
            "sip_config_is_nat_enabled_bool";

    /**
     * UE's public IPaddress when UE is behind a NAT.
     * <p>
     * This key will not exist if {@link #KEY_SIP_CONFIG_IS_NAT_ENABLED_BOOL} is {@code false}.
     */
    public static final String KEY_SIP_CONFIG_UE_PUBLIC_IPADDRESS_WITH_NAT_STRING =
            "sip_config_ue_public_ipaddress_with_nat_string";

    /**
     * UE's public SIP port when UE is behind a NAT.
     * <p>
     * This key will not exist if {@link #KEY_SIP_CONFIG_IS_NAT_ENABLED_BOOL} is {@code false}.
     */
    public static final String KEY_SIP_CONFIG_UE_PUBLIC_PORT_WITH_NAT_INT =
            "sip_config_ue_public_port_with_nat_int";

    /**
     * Flag specifying if Globally routable user-agent uri (GRUU) is enabled as per TS 23.808
     */
    public static final String KEY_SIP_CONFIG_IS_GRUU_ENABLED_BOOL =
            "sip_config_is_gruu_enabled_bool";

    /**
     * UE's Globally routable user-agent uri if this feature is enabled.
     * <p>
     * This key will not exist if {@link #KEY_SIP_CONFIG_IS_GRUU_ENABLED_BOOL} is {@code false}.
     */
    public static final String KEY_SIP_CONFIG_UE_PUBLIC_GRUU_STRING =
            "sip_config_ue_public_gruu_string";

    /**
     * Flag specifying if SIP over IPSec is enabled.
     */
    public static final String KEY_SIP_CONFIG_IS_IPSEC_ENABLED_BOOL =
            "sip_config_is_ipsec_enabled_bool";
    /**
     * UE's SIP port used to send traffic when IPSec is enabled.
     * <p>
     * This key will not exist if {@link #KEY_SIP_CONFIG_IS_IPSEC_ENABLED_BOOL} is {@code false}.
     */
    public static final String KEY_SIP_CONFIG_UE_IPSEC_CLIENT_PORT_INT =
            "sip_config_ue_ipsec_client_port_int";

    /**
     * UE's SIP port used to receive traffic when IPSec is enabled.
     * <p>
     * This key will not exist if {@link #KEY_SIP_CONFIG_IS_IPSEC_ENABLED_BOOL} is {@code false}.
     */
    public static final String KEY_SIP_CONFIG_UE_IPSEC_SERVER_PORT_INT =
            "sip_config_ue_ipsec_server_port_int";

    /**
     * UE's SIP port used for the previous IPsec security association if IPSec is enabled.
     * <p>
     * This key will not exist if {@link #KEY_SIP_CONFIG_IS_IPSEC_ENABLED_BOOL} is {@code false}.
     */
    public static final String KEY_SIP_CONFIG_UE_IPSEC_OLD_CLIENT_PORT_INT =
            "sip_config_ue_ipsec_old_client_port_int";

    /**
     * Port number used by the SIP server to send SIP traffic when IPSec is enabled.
     * <p>
     * This key will not exist if {@link #KEY_SIP_CONFIG_IS_IPSEC_ENABLED_BOOL} is {@code false}.
     */
    public static final String KEY_SIP_CONFIG_SERVER_IPSEC_CLIENT_PORT_INT =
            "sip_config_server_ipsec_client_port_int";

    /**
     * Port number used by the SIP server to receive incoming SIP traffic when IPSec is enabled.
     * <p>
     * This key will not exist if {@link #KEY_SIP_CONFIG_IS_IPSEC_ENABLED_BOOL} is {@code false}.
     */
    public static final String KEY_SIP_CONFIG_SERVER_IPSEC_SERVER_PORT_INT =
            "sip_config_server_ipsec_server_port_int";

    /**
     * Port number used by the SIP server to send SIP traffic on the previous IPSec security
     * association when IPSec is enabled.
     * <p>
     * This key will not exist if {@link #KEY_SIP_CONFIG_IS_IPSEC_ENABLED_BOOL} is {@code false}.
     */
    public static final String KEY_SIP_CONFIG_SERVER_IPSEC_OLD_CLIENT_PORT_INT =
            "sip_config_server_ipsec_old_client_port_int";
    /**
     * SIP Authentication header string
     */
    public static final String KEY_SIP_CONFIG_AUTHENTICATION_HEADER_STRING =
            "sip_config_auhentication_header_string";

    /**
     * SIP Authentication nonce string
     */
    public static final String KEY_SIP_CONFIG_AUTHENTICATION_NONCE_STRING =
            "sip_config_authentication_nonce_string";

    /**
     * SIP service route header string
     */
    public static final String KEY_SIP_CONFIG_SERVICE_ROUTE_HEADER_STRING =
            "sip_config_service_route_header_string";

    /**
     * SIP security verify header string
     */
    public static final String KEY_SIP_CONFIG_SECURITY_VERIFY_HEADER_STRING =
            "sip_config_security_verify_header_string";

    /**
     * SIP Path header string
     */
    public static final String KEY_SIP_CONFIG_PATH_HEADER_STRING =
            "sip_config_path_header_string";

    /**
     * SIP User part string in contact header
     */
    public static final String KEY_SIP_CONFIG_URI_USER_PART_STRING =
            "sip_config_uri_user_part_string";

    /**
     * SIP P-access-network-info header string
     */
    public static final String KEY_SIP_CONFIG_P_ACCESS_NETWORK_INFO_HEADER_STRING =
            "sip_config_p_access_network_info_header_string";

    /**
     * SIP P-last-access-network-info header string
     */
    public static final String KEY_SIP_CONFIG_P_LAST_ACCESS_NETWORK_INFO_HEADER_STRING =
            "sip_config_p_last_access_network_info_header_string";

    /**
     * SIP P-associated-uri header string
     */
    public static final String KEY_SIP_CONFIG_P_ASSOCIATED_URI_HEADER_STRING =
            "sip_config_p_associated_uri_header_string";

    /**@hide*/
    @StringDef(prefix = "KEY_SIP_CONFIG", suffix = "_STRING", value = {
            KEY_SIP_CONFIG_TRANSPORT_TYPE_STRING,
            KEY_SIP_CONFIG_UE_PUBLIC_USER_ID_STRING,
            KEY_SIP_CONFIG_UE_PRIVATE_USER_ID_STRING,
            KEY_SIP_CONFIG_HOME_DOMAIN_STRING,
            KEY_SIP_CONFIG_IMEI_STRING,
            KEY_SIP_CONFIG_IPTYPE_STRING,
            KEY_SIP_CONFIG_UE_DEFAULT_IPADDRESS_STRING,
            KEY_SIP_CONFIG_SERVER_DEFAULT_IPADDRESS_STRING,
            KEY_SIP_CONFIG_UE_PUBLIC_IPADDRESS_WITH_NAT_STRING,
            KEY_SIP_CONFIG_UE_PUBLIC_GRUU_STRING,
            KEY_SIP_CONFIG_AUTHENTICATION_HEADER_STRING,
            KEY_SIP_CONFIG_AUTHENTICATION_NONCE_STRING,
            KEY_SIP_CONFIG_SERVICE_ROUTE_HEADER_STRING,
            KEY_SIP_CONFIG_SECURITY_VERIFY_HEADER_STRING,
            KEY_SIP_CONFIG_PATH_HEADER_STRING,
            KEY_SIP_CONFIG_URI_USER_PART_STRING,
            KEY_SIP_CONFIG_P_ACCESS_NETWORK_INFO_HEADER_STRING,
            KEY_SIP_CONFIG_P_LAST_ACCESS_NETWORK_INFO_HEADER_STRING,
            KEY_SIP_CONFIG_P_ASSOCIATED_URI_HEADER_STRING
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface StringConfigKey {}

    /**@hide*/
    @StringDef(prefix = "KEY_SIP_CONFIG", suffix = "_INT", value = {
            KEY_SIP_CONFIG_MAX_PAYLOAD_SIZE_ON_UDP_INT,
            KEY_SIP_CONFIG_UE_DEFAULT_PORT_INT,
            KEY_SIP_CONFIG_SERVER_DEFAULT_PORT_INT,
            KEY_SIP_CONFIG_UE_PUBLIC_PORT_WITH_NAT_INT,
            KEY_SIP_CONFIG_UE_IPSEC_CLIENT_PORT_INT,
            KEY_SIP_CONFIG_UE_IPSEC_SERVER_PORT_INT,
            KEY_SIP_CONFIG_UE_IPSEC_OLD_CLIENT_PORT_INT,
            KEY_SIP_CONFIG_SERVER_IPSEC_CLIENT_PORT_INT,
            KEY_SIP_CONFIG_SERVER_IPSEC_SERVER_PORT_INT,
            KEY_SIP_CONFIG_SERVER_IPSEC_OLD_CLIENT_PORT_INT
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface IntConfigKey {}

    /**@hide*/
    @StringDef(prefix = "KEY_SIP_CONFIG", suffix = "_BOOL", value = {
            KEY_SIP_CONFIG_IS_COMPACT_FORM_ENABLED_BOOL,
            KEY_SIP_CONFIG_IS_KEEPALIVE_ENABLED_BOOL,
            KEY_SIP_CONFIG_IS_NAT_ENABLED_BOOL,
            KEY_SIP_CONFIG_IS_GRUU_ENABLED_BOOL,
            KEY_SIP_CONFIG_IS_IPSEC_ENABLED_BOOL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface BooleanConfigKey {}

    /**
     * Builder class to be used when constructing a new SipDelegateImsConfiguration.
     */
    public static final class Builder {
        private final long mVersion;
        private final PersistableBundle mBundle;

        /**
         * Creates an empty implementation of SipDelegateImsConfiguration.
         * @param version The version associated with the SipDelegateImsConfiguration being built.
         *                See {@link #getVersion} for more information.
         */
        public Builder(int version) {
            mVersion = version;
            mBundle = new PersistableBundle();
        }
        /**
         * Clones an existing implementation of SipDelegateImsConfiguration to handle situations
         * where only a small number of parameters have changed from the previous configuration.
         * <p>
         * Automatically increments the version of this configuration by 1. See {@link #getVersion}
         * for more information.
         */
        public Builder(@NonNull SipDelegateImsConfiguration config) {
            mVersion = config.getVersion() + 1;
            mBundle = config.copyBundle();
        }
        /**
         * Put a string value into this configuration bundle for the given key.
         */
        // getString is available below.
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder addString(@NonNull @StringConfigKey String key,
                @NonNull String value) {
            mBundle.putString(key, value);
            return this;
        }

        /**
         * Replace the existing default value with a new value for a given key.
         */
        // getInt is available below.
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder addInt(@NonNull @IntConfigKey String key, int value) {
            mBundle.putInt(key, value);
            return this;
        }

        /**
         * Replace the existing default value with a new value for a given key.
         */
        // getBoolean is available below.
        @SuppressLint("MissingGetterMatchingBuilder")
        public @NonNull Builder addBoolean(@NonNull @BooleanConfigKey String key, boolean value) {
            mBundle.putBoolean(key, value);
            return this;
        }

        /**
         * @return a new SipDelegateImsConfiguration from this Builder.
         */
        public @NonNull SipDelegateImsConfiguration build() {
            return new SipDelegateImsConfiguration(mVersion, mBundle);
        }
    }

    private final long mVersion;
    private final PersistableBundle mBundle;

    private SipDelegateImsConfiguration(long version, PersistableBundle bundle) {
        mVersion = version;
        mBundle = bundle;
    }

    private SipDelegateImsConfiguration(Parcel source) {
        mVersion = source.readLong();
        mBundle = source.readPersistableBundle();
    }

    /**
     * @return {@code true} if this configuration object has a an entry for the key specified,
     * {@code false} if it does not.
     */
    public boolean containsKey(@NonNull String key) {
        return mBundle.containsKey(key);
    }

    /**
     * @return the string value associated with a given key or {@code null} if it doesn't exist.
     */
    public @Nullable @StringConfigKey String getString(@NonNull String key) {
        return mBundle.getString(key);
    }

    /**
     * @return the integer value associated with a given key if it exists or the supplied default
     * value if it does not.
     */
    public @IntConfigKey int getInt(@NonNull String key, int defaultValue) {
        if (!mBundle.containsKey(key)) {
            return defaultValue;
        }
        return mBundle.getInt(key);
    }

    /**
     * @return the boolean value associated with a given key or the supplied default value if the
     * value doesn't exist in the bundle.
     */
    public @BooleanConfigKey boolean getBoolean(@NonNull String key, boolean defaultValue) {
        if (!mBundle.containsKey(key)) {
            return defaultValue;
        }
        return mBundle.getBoolean(key);
    }

    /**
     * @return a shallow copy of the full configuration.
     */
    public @NonNull PersistableBundle copyBundle() {
        return new PersistableBundle(mBundle);
    }

    /**
     * An integer representing the version number of this SipDelegateImsConfiguration.
     * {@link SipMessage}s that are created using this configuration will also have a this
     * version number associated with them, which will allow the IMS service to validate that the
     * {@link SipMessage} was using the latest configuration during creation and not a stale
     * configuration due to race conditions between the configuration being updated and the RCS
     * application not receiving the updated configuration before generating a new message.
     *
     * @return the version number associated with this {@link SipDelegateImsConfiguration}.
     */
    public long getVersion() {
        return mVersion;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(mVersion);
        dest.writePersistableBundle(mBundle);
    }

    public static final @NonNull Creator<SipDelegateImsConfiguration> CREATOR =
            new Creator<SipDelegateImsConfiguration>() {
        @Override
        public SipDelegateImsConfiguration createFromParcel(Parcel source) {
            return new SipDelegateImsConfiguration(source);
        }

        @Override
        public SipDelegateImsConfiguration[] newArray(int size) {
            return new SipDelegateImsConfiguration[size];
        }
    };
}
