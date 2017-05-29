library ieee;
  use ieee.std_logic_1164.all;
  use ieee.numeric_std.all;

library work;
  use work.jor.all;
  use work.tb_rams.all;

entity tb_top is
end tb_top;

architecture tb of tb_top is

    signal clk   : std_logic;
    signal rst   : std_logic;
    signal start : std_logic;
    signal busy  : std_logic;
    signal done  : std_logic;
    signal error : std_logic;
    signal init_cr : unsigned(JOR_HOST_ADDR_BITS-1 downto 0);
    signal dh_o_tb : data_handler_out := data_handler_out_init;
    signal dh_i_tb : data_handler_in := data_handler_in_init;

    constant tb_period : time := 4 ns;
    constant tb_host_latency : integer := 1;
    signal tb_clk : std_logic := '0';
    signal tb_end : std_logic := '0';

begin

    dut : entity work.top
    port map (clk     => clk,
              rst     => rst,
              start   => start,
              busy    => busy,
              done    => done,
              error   => error,
              init_cr => init_cr,
              dh_o_tb => dh_o_tb,
              dh_i_tb => dh_i_tb);

    tb_clk <= not tb_clk after tb_period/2 when tb_end /= '1' else '0';
    clk <= tb_clk;

    datahandler : process
    begin
      if tb_end = '0' then
        wait until dh_i_tb.valid = '1';
        wait for tb_period;
        dh_o_tb.done <= '0';
        wait for (to_integer(unsigned(dh_i_tb.size))/8+tb_host_latency)*tb_period;
        dh_o_tb.id <= dh_i_tb.id;
        dh_o_tb.done <= '1';
      else
        wait;
      end if;
    end process;

    stimuli : process
    begin
        start <= '0';
        init_cr <= CR_TESTBENCH;

        rst <= '1';
        wait for tb_period + tb_period/2;
        rst <= '0';
        wait for tb_period;
        start <= '1';
        wait for tb_period;
        start <= '0';

        wait until done = '1';

        tb_end <= '1';
        wait;
    end process;

end tb;
