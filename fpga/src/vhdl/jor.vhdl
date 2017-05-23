library ieee;
  use ieee.std_logic_1164.all;
  use ieee.numeric_std.all;

library work;

package jor is

  constant JOR_HOST_ADDR_BITS : integer := 64;    

  constant JOR_CCL_RAM_SIZE   : integer := 256;
  constant JOR_CCL_ADDR_BITS  : integer := 8;
  constant JOR_CCL_INSTR_SIZE : integer := 32;
  
  constant JOR_LC_ADDR_BITS   : integer := 16;
  constant JOR_OS_BITS        : integer := JOR_LC_ADDR_BITS;
  
  constant JOR_STACK_SIZE     : integer := 32;  

  type stack_item is record 
    cclc  : std_logic_vector(JOR_CCL_ADDR_BITS-1 downto 0);
    cop   : std_logic_vector(JOR_LC_ADDR_BITS-1 downto 0);
    cr    : std_logic_vector(JOR_HOST_ADDR_BITS - 1 downto 0);
  end record;
  
  constant stack_item_empty : stack_item := (
    cclc  => (others => '0'), 
    cop   => (others => '0'), 
    cr    => (others => '0')
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
  
  type ccl_in is record
    clka  : std_logic;
    clkb  : std_logic;
    ena   : std_logic;
    enb   : std_logic;
    wea   : std_logic;
    addra : std_logic_vector(JOR_CCL_ADDR_BITS-1 downto 0);
    addrb : std_logic_vector(JOR_CCL_ADDR_BITS-1 downto 0);
    dia   : std_logic_vector(JOR_CCL_INSTR_SIZE-1 downto 0);
  end record;
  
  type ccl_out is record
    dob   : std_logic_vector(JOR_CCL_INSTR_SIZE-1 downto 0);
  end record;
  
  type jor_state is (Idle, Instance, TypeArray, ObjectArray, Reference, EndOfClass);
  
  type jor_regs is record
    cos   : unsigned(JOR_OS_BITS-1 downto 0);
    tail  : unsigned(JOR_OS_BITS-1 downto 0);
    cclc  : unsigned(JOR_CCL_ADDR_BITS-1 downto 0);
    cop   : unsigned(JOR_LC_ADDR_BITS-1 downto 0);
    cr    : unsigned(JOR_HOST_ADDR_BITS-1 downto 0);
    instr : std_logic_vector(JOR_CCL_INSTR_SIZE-1 downto 0);
    stack : stack_in;
  end record;
  
  constant jor_regs_init : jor_regs := (
    cos   => (others => '0'),
    tail  => (others => '0'),
    cclc  => (others => '0'),
    cop   => (others => '0'),
    cr    => (others => '0'),
    instr => (others => '0'),
    stack => stack_in_init
  ); 
  
  constant JOR_CLASS_BIT : integer := JOR_CCL_INSTR_SIZE-1;
  
  constant JOR_OPCODE_BITS : integer := 3;
  constant JOR_REFE : std_logic_vector(JOR_OPCODE_BITS-1 downto 0) := "100";
  constant JOR_AOBJ : std_logic_vector(JOR_OPCODE_BITS-1 downto 0) := "101";
  constant JOR_ATYP : std_logic_vector(JOR_OPCODE_BITS-1 downto 0) := "111";
  constant JOR_EOCL : std_logic_vector(JOR_OPCODE_BITS-1 downto 0) := "110";
  
  type ccl_ram_type is array (0 to JOR_CCL_RAM_SIZE-1) of std_logic_vector(JOR_CCL_INSTR_SIZE-1 downto 0);
  
  constant JOR_REFE_INDEX_TOP : integer := 7;
  constant JOR_REFE_INDEX_BOT : integer := 0;
  
  constant CCL_RAM_TESTBENCH : ccl_ram_type := (
   0 => "00000000000000000000000000011000", --   0: CLAS 24          # root
   1 => "10000000000000000000110000000100", --   1:   REFE 12, 4     # Ref to leaf
   2 => "10000000000000000001000000000100", --   2:   REFE 16, 4     # Ref to leaf
   3 => "11000000000000000000000000000000", --   3: EOCL             # End of class
   4 => "00000000000000000000000000011000", --   4: CLAS 24          # leaf
   5 => "11000000000000000000000000000000", --   5: EOCL             # End of class
    others => (others => '0')
  );
  
end package;

