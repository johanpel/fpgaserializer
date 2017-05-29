library ieee;
  use ieee.std_logic_1164.all;
  use ieee.numeric_std.all;

library work;
  use work.jor.all;
  use work.utils.all;

package tb_rams is
---------------------------------------------------------------------------- TESTBENCH RAM CONTENTS

---------------------------------------------------------------------- SIMPLE OBJECT AND TYPE ARRAY

  constant TB_CLAS_ATYP_CR : unsigned(JOR_HOST_ADDR_BITS-1 downto 0) := X"5007000000000000";

  constant TB_CLAS_ATYP_CCL : ccl_ram_type := (
    0 => "00000000000000000000000000100000", --   0: CLAS 32          # org.tudelft.ewi.ce.fpgaserialize.root
    1 => "10000000000000000001000000000100", --   1:   REFE 16, 4     # Ref to org.tudelft.ewi.ce.fpgaserialize.leaf
    2 => "10000000000000000001100000000110", --   2:   REFE 24, 6     # Ref to [I
    3 => "11000000000000000000000000000000", --   3: EOCL             # End of class
    4 => "00000000000000000000000000100000", --   4: CLAS 32          # org.tudelft.ewi.ce.fpgaserialize.leaf
    5 => "11000000000000000000000000000000", --   5: EOCL             # End of class
    6 => "11100000000000000000010000000000", --   6: ATYP 4           # [I
    7 => "11000000000000000000000000000000", --   7: EOCL             # End of class
    others => (others => '0')
  );
    
  constant TB_CLAS_ATYP_LC : lc_ram_type := (
     0 => X"FFFFFFFFFFFFFFFF", -- org.tudelft.ewi.ce.fpgaserialize.root (dc)
     1 => X"FFFFFFFFFFFFFFFF", --   InstanceKlass pointer (dc)
     2 => X"00007F8F6A2A1F10", --   Ref to org.tudelft.ewi.ce.fpgaserialize.leaf
     3 => X"00007F8F6ACBE830", --   Ref to [I
     4 => X"FFFFFFFFFFFFFFFF", -- org.tudelft.ewi.ce.fpgaserialize.leaf (dc)
     5 => X"FFFFFFFFFFFFFFFF", --   InstanceKlass pointer (dc)
     6 => X"00000001" & X"00000002", --   int 1, int 2
     7 => X"00000003" & X"00000000", --   int 3, padding
     8 => X"FFFFFFFFFFFFFFFF", -- [I (dc)
     9 => X"FFFFFFFFFFFFFFFF", --   TypeArrayKlass pointer (dc)
    10 => X"00000008" & X"00000000", --   Array size, padding
    11 => X"AABBCCDD" & X"AABBCCDD", --   int, int
    12 => X"AABBCCDD" & X"AABBCCDD", --   int, int
    13 => X"AABBCCDD" & X"AABBCCDD", --   int, int
    14 => X"AABBCCDD" & X"AABBCCDD", --   int, int
    others => (others => '0')
  );
  
----------------------------------- INSTANCE CLASSES, OBJECT ARRAY OF OBJECT ARRAYS AND TYPE ARRAYS
  
  constant TB_CLAS_AOBJ_ATYP_CR : unsigned(JOR_HOST_ADDR_BITS-1 downto 0) := X"5007000000000000";
  
  constant TB_CLAS_AOBJ_ATYP_CCL : ccl_ram_type := (
    0 => "10100000000000000000100000000010", --   0: AOBJ 2           # Array[[Lorg.tudelft.ewi.ce.fpgaserialize.someClass;]
    1 => "11000000000000000000000000000000", --   1: EOCL             # End of class
    2 => "10100000000000000000100000000100", --   2: AOBJ 4           # Array[org.tudelft.ewi.ce.fpgaserialize.someClass]
    3 => "11000000000000000000000000000000", --   3: EOCL             # End of class
    4 => "00000000000000000000000000100000", --   4: CLAS 32          # org.tudelft.ewi.ce.fpgaserialize.someClass
    5 => "10000000000000000001100000000111", --   5:   REFE 24, 7     # Ref to [I
    6 => "11000000000000000000000000000000", --   6: EOCL             # End of class
    7 => "11100000000000000000010000000000", --   7: ATYP 4           # [I
    8 => "11000000000000000000000000000000", --   8: EOCL             # End of class
    others => (others => '0')
  );
 
  constant TB_CLAS_AOBJ_ATYP_LC : lc_ram_type := (
       --OOP: 5007000000000000                Array[[Lorg.tudelft.ewi.ce.fpgaserialize.someClass;]
    00 => X"FFFFFFFFFFFFFFFF",
    01 => X"FFFFFFFFFFFFFFFF",
    02 => bs32(X"02000000") & X"00000000",
    03 => X"C100000000000000",
    04 => X"C200000000000000",
       --OOP: C100000000000000                Array[org.tudelft.ewi.ce.fpgaserialize.someClass]
    05 => X"FFFFFFFFFFFFFFFF",
    06 => X"FFFFFFFFFFFFFFFF",
    07 => bs32(X"02000000") & X"00000000",
    08 => X"C1C1000000000000",
    09 => X"C1C2000000000000",
       --OOP: C1C1000000000000                org.tudelft.ewi.ce.fpgaserialize.someClass
    10 => X"FFFFFFFFFFFFFFFF",
    11 => X"FFFFFFFFFFFFFFFF",
    12 => X"11111111" & X"22222222",
    13 => X"C1C1C10000000000",
       --OOP: C1C1C10000000000                [I
    14 => X"FFFFFFFFFFFFFFFF",
    15 => X"FFFFFFFFFFFFFFFF",
    16 => bs32(X"02000000") & X"00000000",
    17 => X"33333333" & X"33333333",
       --OOP: C1C2000000000000                org.tudelft.ewi.ce.fpgaserialize.someClass
    18 => X"FFFFFFFFFFFFFFFF",
    19 => X"FFFFFFFFFFFFFFFF",
    20 => X"44444444" & X"55555555",
    21 => X"C1C2C10000000000",
       --OOP: C1C2C10000000000                [I
    22 => X"FFFFFFFFFFFFFFFF",
    23 => X"FFFFFFFFFFFFFFFF",
    24 => bs32(X"01000000") & X"00000000",
    25 => X"66666666" & X"FFFFFFFF",
       --OOP: C200000000000000                Array[org.tudelft.ewi.ce.fpgaserialize.someClass]
    26 => X"FFFFFFFFFFFFFFFF",
    27 => X"FFFFFFFFFFFFFFFF",
    28 => bs32(X"02000000") & X"00000000",
    29 => X"C2C1000000000000",
    30 => X"C2C2000000000000",
       --OOP: C2C1000000000000                org.tudelft.ewi.ce.fpgaserialize.someClass
    31 => X"FFFFFFFFFFFFFFFF",
    32 => X"FFFFFFFFFFFFFFFF",
    33 => X"77777777" & X"88888888",
    34 => X"C2C1C10000000000",
       --OOP: C2C1C10000000000                [I
    35 => X"FFFFFFFFFFFFFFFF",
    36 => X"FFFFFFFFFFFFFFFF",
    37 => bs32(X"03000000") & X"00000000",
    38 => X"99999999" & X"99999999",
    39 => X"99999999" & X"FFFFFFFF",
       --OOP: C2C2000000000000                org.tudelft.ewi.ce.fpgaserialize.someClass
    40 => X"FFFFFFFFFFFFFFFF",
    41 => X"FFFFFFFFFFFFFFFF",
    42 => X"AAAAAAAA" & X"BBBBBBBB",
    43 => X"C2C2C10000000000",
       --OOP: C2C2C10000000000                [I
    44 => X"FFFFFFFFFFFFFFFF",
    45 => X"FFFFFFFFFFFFFFFF",
    46 => bs32(X"04000000") & X"00000000",
    47 => X"CCCCCCCC" & X"CCCCCCCC",
    48 => X"CCCCCCCC" & X"CCCCCCCC",
    others => (others => '0')
  );
  
  
---------------------------------------------------------------------------- TESTBENCH RAM CONTENTS

  --constant CR_TESTBENCH       : unsigned(JOR_HOST_ADDR_BITS-1 downto 0) := TB_CLAS_ATYP_CR;
  --constant CCL_RAM_TESTBENCH  : ccl_ram_type                            := TB_CLAS_ATYP_CCL;
  --constant LC_RAM_TESTBENCH   : lc_ram_type                             := TB_CLAS_ATYP_LC;
  
  constant CR_TESTBENCH       : unsigned(JOR_HOST_ADDR_BITS-1 downto 0) := TB_CLAS_AOBJ_ATYP_CR;
  constant CCL_RAM_TESTBENCH  : ccl_ram_type                            := TB_CLAS_AOBJ_ATYP_CCL;
  constant LC_RAM_TESTBENCH   : lc_ram_type                             := TB_CLAS_AOBJ_ATYP_LC;
end package;

