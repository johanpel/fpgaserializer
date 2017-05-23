library IEEE;
use IEEE.std_logic_1164.all;
use IEEE.std_logic_unsigned.all;

-- Simple Dual-Port Ram Dual Clocks
-- According to Xilinx UG901
entity sdprdc is
  generic(
    RAM_WIDTH : integer := 32;
    RAM_DEPTH : integer := 1024;
    RAM_ADDR_BITS : integer := 10
  );
  port(
    clka  : in std_logic;
    clkb  : in std_logic;
    ena   : in std_logic;
    enb   : in std_logic;
    wea   : in std_logic;
    addra : in std_logic_vector(RAM_ADDR_BITS-1 downto 0);
    addrb : in std_logic_vector(RAM_ADDR_BITS-1 downto 0);
    dia   : in std_logic_vector(RAM_WIDTH-1 downto 0);
    dob   : out std_logic_vector(RAM_WIDTH-1 downto 0)
  );
end sdprdc;

architecture rtl of sdprdc is
  type ram_type is array (RAM_DEPTH-1 downto 0) of std_logic_vector(RAM_WIDTH-1 downto 0);
  shared variable RAM : ram_type;
begin

  -- Write Port A
  process(clka)
  begin
    if rising_edge(clka) then
      if ena = '1' then
        if wea = '1' then
          RAM(conv_integer(addra)) := dia;
        end if;
      end if;
    end if;
  end process;

  -- Read Port B
  process(clkb)
  begin
    if rising_edge(clkb) then
      if enb = '1' then
        dob <= RAM(conv_integer(addrb));
      end if;
    end if;
  end process;

end rtl;
