library ieee;
  use ieee.std_logic_1164.all;
  use ieee.numeric_std.all;

library work;

package jor is
----------------------------------------------------------------------------------------- CONSTANTS
  constant JOR_HOST_ADDR_BITS         : integer :=    64;
  constant JOR_HOST_INDEX_BITS        : integer :=    16;

  constant JOR_CCL_ADDR_BITS          : integer :=     8;
  constant JOR_CCL_RAM_DEPTH          : integer :=   256;                                          -- Must correspond to JOR_CCL_ADDR_BITS
  constant JOR_CCL_INSTR_SIZE         : integer :=    32;

  constant JOR_LC_ADDR_BITS           : integer :=    16;
  constant JOR_LC_RAM_DEPTH           : integer := 65536;                                          -- Must correspond to JOR_LC_ADDR_BITS
  constant JOR_LC_RAM_WIDTH           : integer :=    64;
  constant JOR_LC_RAM_WIDTH_BYTES     : integer := JOR_LC_RAM_WIDTH / 8;

  constant JOR_OBJ_SIZE_BITS          : integer := JOR_LC_ADDR_BITS;

  constant JOR_STACK_SIZE             : integer :=    16;

  constant JOR_ARRAY_HEADER_SIZE      : integer :=    24;
  constant JOR_ARRAY_SIZE_WORD_OFFSET : integer :=     2;
  constant JOR_ARRAY_ELEM_WORD_OFFSET : integer :=     3;
  constant JOR_ARRAY_SW_HI            : integer :=    63; -- Array size high bit in the header word
  constant JOR_ARRAY_SW_LO            : integer :=    32; -- Array size low bit in the header word
  constant JOR_ARRAY_SIZE_BITS        : integer := JOR_ARRAY_SW_HI - JOR_ARRAY_SW_LO + 1;
  constant JOR_ARRAY_SIZE_EMPTY       : unsigned(JOR_ARRAY_SIZE_BITS-1 downto 0) := (others => '0');

-------------------------------------------------------------------------------------- INSTRUCTIONS
  constant JOR_CLASS_BIT              : integer := JOR_CCL_INSTR_SIZE-1;
  constant JOR_OPCODE_BITS            : integer :=  3;

  constant JOR_A_ELEMENT_SIZE_HI      : integer := 11;
  constant JOR_A_ELEMENT_SIZE_LO      : integer :=  8;
  constant JOR_A_ELEMENT_SIZE_BITS    : integer :=  4;

  constant JOR_REFE_OFFSET_HI         : integer := 15;
  constant JOR_REFE_OFFSET_LO         : integer :=  8;

  constant JOR_REFE_INDEX_HI          : integer :=  7;
  constant JOR_REFE_INDEX_LO          : integer :=  0;

  constant JOR_REFE                   : std_logic_vector(JOR_OPCODE_BITS-1 downto 0) := "100";     -- Reference (push to stack)
  constant JOR_AOBJ                   : std_logic_vector(JOR_OPCODE_BITS-1 downto 0) := "101";     -- Array of Objects
  constant JOR_ATYP                   : std_logic_vector(JOR_OPCODE_BITS-1 downto 0) := "111";     -- Array of primitive Types
  constant JOR_EOCL                   : std_logic_vector(JOR_OPCODE_BITS-1 downto 0) := "110";     -- End of Class (pop from stack)

  constant JOR_NOOP                   : std_logic_vector(JOR_CCL_INSTR_SIZE-1 downto 0)            -- No Operation
                                          := (others => '0');

  type jor_instr_type is (CLAS, REFE, AOBJ, ATYP, EOCL, NOOP, ERRO);

------------------------------------------------------------------------------ COMPACT CLASS LAYOUT
  type ccl_ram_in is record
    clka  : std_logic;
    clkb  : std_logic;
    ena   : std_logic;
    enb   : std_logic;
    wea   : std_logic;
    addra : std_logic_vector(JOR_CCL_ADDR_BITS-1 downto 0);
    addrb : std_logic_vector(JOR_CCL_ADDR_BITS-1 downto 0);
    dia   : std_logic_vector(JOR_CCL_INSTR_SIZE-1 downto 0);
  end record;

  type ccl_ram_out is record
    dob   : std_logic_vector(JOR_CCL_INSTR_SIZE-1 downto 0);
  end record;

  type ccl_ram_type is
    array (0 to JOR_CCL_RAM_DEPTH-1)
    of std_logic_vector(JOR_CCL_INSTR_SIZE-1 downto 0);
---------------------------------------------------------------------------------------- LOCAL COPY
  type lc_ram_in is record
    clka  : std_logic;
    clkb  : std_logic;
    ena   : std_logic;
    enb   : std_logic;
    wea   : std_logic;
    addra : std_logic_vector(JOR_LC_ADDR_BITS-1 downto 0);
    addrb : std_logic_vector(JOR_LC_ADDR_BITS-1 downto 0);
    dia   : std_logic_vector(JOR_LC_RAM_WIDTH-1 downto 0);
  end record;

  type lc_ram_out is record
    dob   : std_logic_vector(JOR_LC_RAM_WIDTH-1 downto 0);
  end record;

  type lc_ram_type is
    array (0 to JOR_LC_RAM_DEPTH-1)
    of std_logic_vector(JOR_LC_RAM_WIDTH-1 downto 0);

  type lc_in is record
    addr : std_logic_vector(JOR_LC_ADDR_BITS-1 downto 0);
  end record;

  constant lc_in_init : lc_in := (
    addr => (others => '0')
  );
-------------------------------------------------------------------------------------- DATA HANDLER
  type data_handler_in is record
    valid   : std_logic;
    source  : std_logic_vector(JOR_HOST_ADDR_BITS-1 downto 0);
    dest    : std_logic_vector(JOR_LC_ADDR_BITS-1 downto 0);
    size    : std_logic_vector(JOR_OBJ_SIZE_BITS-1 downto 0);
    id      : std_logic_vector(JOR_HOST_INDEX_BITS-1 downto 0);
  end record;

  constant data_handler_in_init : data_handler_in := (
    valid   => '0',
    source  => (others => '0'),
    dest    => (others => '0'),
    size    => (others => '0'),
    id      => (others => '0')
  );

  type data_handler_out is record
    done    : std_logic;
    id      : std_logic_vector(JOR_HOST_INDEX_BITS-1 downto 0);
  end record;

  constant data_handler_out_init : data_handler_out := (
    done  => '0',
    id    => (others => '0')
  );

------------------------------------------------------------------------------ OBJECT ARRAY SUPPORT
  type object_array is record
    act      : std_logic;                                -- Wether we're actively working on an array of objects
    cac      : unsigned(JOR_OBJ_SIZE_BITS-1 downto 0);   -- Object Array Current Array Counter (index in the current array)
    cas      : unsigned(JOR_OBJ_SIZE_BITS-1 downto 0);   -- Object Array Current Array Size (number total elements in array we're working on)
  end record;

  constant object_array_init : object_array := (
    act => '0',
    cac => (others => '0'),
    cas => (others => '0')
  );

--------------------------------------------------------------------------------------------- STACK
  type stack_item is record
    cclc      : unsigned(JOR_CCL_ADDR_BITS-1 downto 0);
    cop       : unsigned(JOR_LC_ADDR_BITS-1 downto 0);
    cr        : unsigned(JOR_HOST_ADDR_BITS - 1 downto 0);
    oa        : object_array;
  end record;

  constant stack_item_empty : stack_item := (
    cclc  => (others => '0'),
    cop   => (others => '0'),
    cr    => (others => '0'),
    oa    => object_array_init
  );

  type stack_in is record
    clk   : std_logic;
    rst   : std_logic;
    push  : std_logic;
    pop   : std_logic;
    data  : stack_item;
  end record;

  constant stack_in_init : stack_in := (
    clk => '0',
    rst => '1',
    push => '0',
    pop => '1',
    data =>
    stack_item_empty
  );

  type stack_out is record
    data  : stack_item;
    empty : std_logic;
    full  : std_logic;
  end record;

----------------------------------------------------------------------------------------- REGISTERS
  type jor_components is record
    stack : stack_in;
    dh    : data_handler_in;
    lc    : lc_in;
  end record;

  constant jor_components_init : jor_components := (
    stack => stack_in_init,
    dh    => data_handler_in_init,
    lc    => lc_in_init
  );

  type jor_regs is record
    cop         : unsigned(JOR_LC_ADDR_BITS-1 downto 0);    -- Current Object Pointer (in the local copy)
    cos         : unsigned(JOR_OBJ_SIZE_BITS-1 downto 0);   -- Current Object Size
    tail        : unsigned(JOR_OBJ_SIZE_BITS-1 downto 0);   -- End of serialized root object
    cclc        : unsigned(JOR_CCL_ADDR_BITS-1 downto 0);   -- Compact Class Layout Counter (like a program counter)
    cr          : unsigned(JOR_HOST_ADDR_BITS-1 downto 0);  -- Current reference (in the host memory)
    reqid       : unsigned(JOR_HOST_INDEX_BITS-1 downto 0); -- Request ID to the data handler
    stall       : std_logic;                                -- Stalling for object access to the data handler
    array_stall : std_logic;                                -- Stalling for array access to the data handler
    done        : std_logic;
    error       : std_logic;
    oa          : object_array;                             -- Registers for object array support
    comp        : jor_components;
  end record;

  constant jor_regs_init : jor_regs := (
    cos         => (others => '0'),
    tail        => (others => '0'),
    cclc        => (others => '0'),
    cop         => (others => '0'),
    cr          => (others => '0'),
    reqid       => (others => '0'),
    stall       => '0',
    array_stall => '0',
    error       => '0',
    done        => '1',
    oa          => object_array_init,
    comp        => jor_components_init
  );
end package;

