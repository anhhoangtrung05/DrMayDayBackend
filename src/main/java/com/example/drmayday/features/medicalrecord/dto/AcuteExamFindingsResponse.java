package com.example.drmayday.features.medicalrecord.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AcuteExamFindingsResponse {

    @JsonProperty("exam_id")
    private String examId;

    @JsonProperty("loai_benh_an")
    @Builder.Default
    private String loaiBenhAn = "CAP";

    @JsonProperty("co_san_phu")
    private Boolean coSanPhu;

    @JsonProperty("co_anh_san_phu")
    private Boolean coAnhSanPhu;

    @JsonProperty("trieu_chung_da_niem_mac")
    private Boolean trieuChungDaNiemMac;

    @JsonProperty("trieu_chung_da_ghi_ro")
    private String trieuChungDaGhiRo;

    @JsonProperty("trieu_chung_ho_hap")
    private Boolean trieuChungHoHap;

    @JsonProperty("trieu_chung_ho_hap_ghi_ro")
    private String trieuChungHoHapGhiRo;

    @JsonProperty("trieu_chung_tuan_hoan")
    private Boolean trieuChungTuanHoan;

    @JsonProperty("trieu_chung_tuan_hoan_ghi_ro")
    private String trieuChungTuanHoanGhiRo;

    @JsonProperty("trieu_chung_tieu_hoa")
    private Boolean trieuChungTieuHoa;

    @JsonProperty("trieu_chung_tieu_hoa_ghi_ro")
    private String trieuChungTieuHoaGhiRo;

    @JsonProperty("so_lan_non")
    private Integer soLanNon;

    @JsonProperty("trieu_chung_khac")
    private String trieuChungKhac;
}
