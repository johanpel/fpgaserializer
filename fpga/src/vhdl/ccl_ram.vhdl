library IEEE;
  use IEEE.std_logic_1164.all;
  use IEEE.std_logic_unsigned.all;

library work;
  use work.jor.all;

-- Simple Dual-Port Ram Dual Clocks
-- According to Xilinx UG901
entity ccl_ram is
  generic(
    CCL_RAM_INIT : ccl_ram_type := (others => (others => '0'))
  );
  port(
    clka  : in std_logic;
    clkb  : in std_logic;
    ena   : in std_logic;
    enb   : in std_logic;
    wea   : in std_logic;
    addra : in std_logic_vector(JOR_CCL_ADDR_BITS-1 downto 0);
    addrb : in std_logic_vector(JOR_CCL_ADDR_BITS-1 downto 0);
    dia   : in std_logic_vector(JOR_CCL_INSTR_SIZE-1 downto 0);
    dob   : out std_logic_vector(JOR_CCL_INSTR_SIZE-1 downto 0)
  );
end ccl_ram;

architecture rtl of ccl_ram is
  signal RAM : ccl_ram_type := CCL_RAM_INIT;
begin

  -- Write Port A
  process(clka)
  begin
    if rising_edge(clka) then
      if ena = '1' then
        if wea = '1' then
          RAM(conv_integer(addra)) <= dia;
        end if;
      end if;
    end if;
  end process;

  dob <= RAM(conv_integer(addrb));

end rtl;
