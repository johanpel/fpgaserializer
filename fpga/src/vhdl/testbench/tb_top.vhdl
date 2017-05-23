library ieee;
  use ieee.std_logic_1164.all;
  use ieee.numeric_std.all;
  
library work;
  use work.jor.all;

entity tb_top is
end tb_top;

architecture tb of tb_top is

    component top
        port (clk   : in std_logic;
              rst   : in std_logic;
              start : in std_logic;
              busy  : out std_logic;
              done  : out std_logic;
              error : out std_logic;
              init_cr : unsigned(JOR_HOST_ADDR_BITS-1 downto 0)
              );
    end component;

    signal clk   : std_logic;
    signal rst   : std_logic;
    signal start : std_logic;
    signal busy  : std_logic;
    signal done  : std_logic;
    signal error : std_logic;
    signal init_cr : unsigned(JOR_HOST_ADDR_BITS-1 downto 0);

    constant tb_period : time := 100 ns;
    signal tb_clk : std_logic := '0';
    signal tb_end : std_logic := '0';

begin

    dut : top
    port map (clk     => clk,
              rst     => rst,
              start   => start,
              busy    => busy,
              done    => done,
              error   => error,
              init_cr => init_cr);

    tb_clk <= not tb_clk after tb_period/2 when tb_end /= '1' else '0';
    clk <= tb_clk;

    stimuli : process
    begin
        start <= '0';
        init_cr <= X"FEDCBA9800000000";

        rst <= '1';
        wait for tb_period;
        rst <= '0';
        wait for tb_period;

        wait for 100*tb_period;

        tb_end <= '1';
        wait;
    end process;

end tb;
