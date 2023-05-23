package com.cc.pojo;

import com.cc.codegen.processor.vo.FieldDesc;
import com.cc.codegen.processor.vo.GenVo;
import com.cc.codegen.processor.vo.IgnoreVo;
import lombok.Data;

@Data
@GenVo(pkgName = "com.cc.vo")
public class User {

    private Long id;

    @FieldDesc(name = "named")
    private String name;

    @IgnoreVo
    private String desc;

}
