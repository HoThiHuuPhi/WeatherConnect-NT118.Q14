package com.example.doanck.utils

object AdminMap {

    val MERGED_PROVINCE_MAP = mapOf(
        "Hà Giang" to "tỉnh Tuyên Quang",
        "Tuyên Quang" to "tỉnh Tuyên Quang",
        "Yên Bái" to "tỉnh Lào Cai",
        "Lào Cai" to "tỉnh Lào Cai",
        "Bắc Kạn" to "tỉnh Thái Nguyên",
        "Thái Nguyên" to "tỉnh Thái Nguyên",

        "Vĩnh Phúc" to "tỉnh Phú Thọ",
        "Hòa Bình" to "tỉnh Phú Thọ",
        "Phú Thọ" to "tỉnh Phú Thọ",
        "Bắc Giang" to "tỉnh Bắc Ninh",
        "Bắc Ninh" to "tỉnh Bắc Ninh",
        "Thái Bình" to "tỉnh Hưng Yên",
        "Hưng Yên" to "tỉnh Hưng Yên",

        "Hải Dương" to "Thành phố Hải Phòng",
        "Hải Phòng" to "Thành phố Hải Phòng",
        "Hà Nam" to "tỉnh Ninh Bình",
        "Nam Định" to "tỉnh Ninh Bình",
        "Ninh Bình" to "tỉnh Ninh Bình",
        "Quảng Bình" to "tỉnh Quảng Trị",
        "Quảng Trị" to "tỉnh Quảng Trị",

        "Quảng Nam" to "Thành phố Đà Nẵng",
        "Đà Nẵng" to "Thành phố Đà Nẵng",
        "Kon Tum" to "tỉnh Quảng Ngãi",
        "Quảng Ngãi" to "tỉnh Quảng Ngãi",
        "Bình Định" to "tỉnh Gia Lai",
        "Gia Lai" to "tỉnh Gia Lai",
        "Ninh Thuận" to "tỉnh Khánh Hoà",
        "Khánh Hoà" to "tỉnh Khánh Hoà",
        "Đắk Nông" to "tỉnh Lâm Đồng",
        "Bình Thuận" to "tỉnh Lâm Đồng",
        "Lâm Đồng" to "tỉnh Lâm Đồng",
        "Phú Yên" to "tỉnh Đắk Lắk",
        "Đắk Lắk" to "tỉnh Đắk Lắk",

        "Bà Rịa - Vũng Tàu" to "Thành phố Hồ Chí Minh",
        "Bình Dương" to "Thành phố Hồ Chí Minh",
        "Hồ Chí Minh" to "Thành phố Hồ Chí Minh",
        "Bình Phước" to "tỉnh Đồng Nai",
        "Đồng Nai" to "tỉnh Đồng Nai",
        "Long An" to "tỉnh Tây Ninh",
        "Tây Ninh" to "tỉnh Tây Ninh",
        "Sóc Trăng" to "Thành phố Cần Thơ",
        "Hậu Giang" to "Thành phố Cần Thơ",
        "Cần Thơ" to "Thành phố Cần Thơ",
        "Bến Tre" to "tỉnh Vĩnh Long",
        "Trà Vinh" to "tỉnh Vĩnh Long",
        "Vĩnh Long" to "tỉnh Vĩnh Long",
        "Tiền Giang" to "tỉnh Đồng Tháp",
        "Đồng Tháp" to "tỉnh Đồng Tháp",
        "Bạc Liêu" to "tỉnh Cà Mau",
        "Cà Mau" to "tỉnh Cà Mau",
        "Kiên Giang" to "tỉnh An Giang",
        "An Giang" to "tỉnh An Giang",

        "Hà Nội" to "Thành phố Hà Nội",
        "Huế" to "Thành phố Huế",
        "Cao Bằng" to "tỉnh Cao Bằng",
        "Lai Châu" to "tỉnh Lai Châu",
        "Điện Biên" to "tỉnh Điện Biên",
        "Lạng Sơn" to "tỉnh Lạng Sơn",
        "Sơn La" to "tỉnh Sơn La",
        "Quảng Ninh" to "tỉnh Quảng Ninh",
        "Thanh Hóa" to "tỉnh Thanh Hóa",
        "Nghệ An" to "tỉnh Nghệ An",
        "Hà Tĩnh" to "tỉnh Hà Tĩnh"
    )

    fun getNewProvinceName(currentProvinceName: String?): String? {
        if (currentProvinceName == null) return null
        val cleanedName = currentProvinceName.replace("Tỉnh ", "", ignoreCase = true).replace("Thành phố ", "", ignoreCase = true).trim()
        return MERGED_PROVINCE_MAP[cleanedName] ?: currentProvinceName
    }
}