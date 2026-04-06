# The RTL8821CU is a combo WiFi+BT USB chip. The upstream linux-firmware
# linux-firmware-rtl8821 package includes the WiFi firmware (rtw88/ and
# rtlwifi/) but omits the Bluetooth firmware (rtl_bt/). Add the missing
# BT firmware and config files so the kernel's btusb/hci_uart driver can
# load them for the Bluetooth half of the chip.
FILES:${PN}-rtl8821 += " \
  ${nonarch_base_libdir}/firmware/rtl_bt/rtl8821c_fw.bin \
  ${nonarch_base_libdir}/firmware/rtl_bt/rtl8821c_config.bin \
"
