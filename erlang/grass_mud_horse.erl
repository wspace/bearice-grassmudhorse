%% (c) Copyright Bearice 2010.  All rights reserved.
%% See COPYING for more copyrights details
%% TODO: Add description to grass_mud_horse

-module(grass_mud_horse).

%%
%% Include files
%%
-include("grass_mud_horse.hrl").
%%
%% Exported Functions
%%
-export([compile_run/1,c/1,load/1,r/1]).

%%
%% API Functions
%%
compile_run(Filename)->
	case c(Filename) of
		{ok,Code}->
			r(Code);
		Err->Err
	end.

c(Filename)->
	case file:read_file(Filename) of
		{ok,Data}->
			compile(tokenize(Data,[]),[]);
		Err->Err
	end.

r(Code)->
	case load(Code) of
		{ok,Code1}->
			run(Code,[],dict:new(),Code1);
		{error,R}->
			{error,R}
	end.

load(Code)->load0(Code,dict:new()).
load0([],Dict)->{ok,Dict};
load0([{defun,L}|Rest],Dict)->
	case dict:find(L, Dict) of
		error ->
			Dict0 = dict:store(L, Rest, Dict),
			load0(Rest,Dict0);
		_ ->
			{error,{duplicated_defuns,L}}
	end;
load0([_|Rest],Dict)->
	load0(Rest,Dict).
run([{push,N}|Rest],Stack,Dict,Code)->
	run(Rest,[N|Stack],Dict,Code);
run([dup|Rest],Stack,Dict,Code)->
	[T|_]=Stack,
	run(Rest,[T|Stack],Dict,Code);
run([{copy,N}|Rest],Stack,Dict,Code)->
	E = lists:nth(N, Stack),
	run(Rest,[E|Stack],Dict,Code);
run([swap|Rest],Stack,Dict,Code)->
	[T1|S1]=Stack,
	[T2|S2]=S1,
	Stack0 = [T2,T1|S2],
	run(Rest,Stack0,Dict,Code);
run([pop|Rest],Stack,Dict,Code)->
	[_|Stack0]=Stack,
	run(Rest,Stack0,Dict,Code);
run([{slide,N}|Rest],Stack,Dict,Code)->
	{[H|_],S0} = lists:split(N+1, Stack),
	run(Rest,[H,S0],Dict,Code);
run([add|Rest],Stack,Dict,Code)->
	[R|S1]=Stack,
	[L|S2]=S1,
	run(Rest,[L+R|S2],Dict,Code);
run([sub|Rest],Stack,Dict,Code)->
	[R|S1]=Stack,
	[L|S2]=S1,
	run(Rest,[L-R|S2],Dict,Code);
run([mul|Rest],Stack,Dict,Code)->
	[R|S1]=Stack,
	[L|S2]=S1,
	run(Rest,[L*R|S2],Dict,Code);
run(['div'|Rest],Stack,Dict,Code)->
	[R|S1]=Stack,
	[L|S2]=S1,
	run(Rest,[L/R|S2],Dict,Code);
run([mod|Rest],Stack,Dict,Code)->
	[R|S1]=Stack,
	[L|S2]=S1,
	run(Rest,[L rem R|S2],Dict,Code);
run([set|Rest],Stack,Dict,Code)->
	[H,N|Stack1]=Stack,
	Dict1 = dict:store(N, H, Dict),
	run(Rest,Stack1,Dict1,Code);
run([load|Rest],Stack,Dict,Code)->
	[N|Stack1]=Stack,
	case catch dict:fetch(N, Dict) of
		X when is_integer(X)->
			run(Rest,[X|Stack1],Dict,Code);
		_ ->
			{error,{'SIGSEGV',{heap,N}}}
	end;

run([{call,L}|Rest],Stack,Dict,Code)->
	case catch dict:fetch(L, Code) of
		CS when is_list(CS)->
			case run(CS,Stack,Dict,Code) of
				{ret,NS,ND}->
					run(Rest,NS,ND,Code);
				ErrorOrExit->
					ErrorOrExit
			end;
		_ ->
			{error,{'SIGSEGV',{code,L}}}
	end;
run([{jmp,L}|_Rest],Stack,Dict,Code)->
	case catch dict:fetch(L, Code) of
		CS when is_list(CS)->
			run(CS,Stack,Dict,Code);
		_ ->
			{error,{'SIGSEGV',{code,L}}}
	end;
run([{jz,L}|Rest],Stack,Dict,Code)->
	[H|Stack0]=Stack,
	case H of
		0->
			case catch dict:fetch(L, Code) of
				CS when is_list(CS)->
					run(CS,Stack0,Dict,Code);
				_ ->
					{error,{'SIGSEGV',{code,L}}}
			end;
		_->
			run(Rest,Stack0,Dict,Code)
	end;
run([{jnz,L}|Rest],Stack,Dict,Code)->
	[H|Stack0]=Stack,
	case H of
		0->
			run(Rest,Stack0,Dict,Code);
		_->
			case catch dict:fetch(L, Code) of
				CS when is_list(CS)->
					run(CS,Stack0,Dict,Code);
				_ ->
					{error,{'SIGSEGV',{code,L}}}
			end
	end;
run([ret|_Rest],Stack,Dict,_Code)->
	{ret,Stack,Dict};
run([exit|_Rest],Stack,Dict,_Code)->
	{exit,Stack,Dict};

run([iint|Rest],Stack,Dict,Code)->
	case io:fread("", "~d") of
		{ok,[Num]}->
			run(Rest,[Num|Stack],Dict,Code);
		{error,E}->
			io:write("Please inuput an integer! (~p)~n",[E]),
			run([iint|Rest],Stack,Dict,Code);
		eof ->
			{error,eof}
  	end;
run([oint|Rest],Stack,Dict,Code)->
	[T|S]=Stack,
	io:format("~p",[T]),
	run(Rest,S,Dict,Code);

run([ichr|Rest],Stack,Dict,Code)->
	case io:get_chars("",1) of
		{ok,[[Num]]}->
			run(Rest,[Num|Stack],Dict,Code);
		{error,E}->
			{error,{'SIGPIPE',E}};
		eof ->
			{error,eof}
  	end;
run([ochr|Rest],Stack,Dict,Code)->
	[T|S]=Stack,
	io:format("~tc",[T]),
	run(Rest,S,Dict,Code);

run([{defun,_}|Rest],Stack,Dict,Code)->
	run(Rest,Stack,Dict,Code);
run([C|_],_Stack,_Dict,_Code)->
	{error,{bad_opcode,C}}.


tokenize(<<C/utf8,Rest/binary>>,Acc)->
	case C of
		?G -> tokenize(Rest,[g|Acc]);
		?M -> tokenize(Rest,[m|Acc]);
		?H -> tokenize(Rest,[h|Acc]);
		?R -> tokenize(Rest,[r|Acc]);
		?C -> tokenize(Rest,[c|Acc]);
		_->tokenize(Rest,Acc)
		end;
tokenize(<<>>,Acc)->lists:reverse(Acc).

compile([],Acc)->
	{ok,lists:reverse(Acc)};
compile(D,Acc)->
	case compile_opc(D) of
		{ok,Code,Rest}->
			compile(Rest,[Code|Acc]);
		{error,What,_}->
			{error,{What,D,lists:reverse(Acc)}}
  	end.

compile_opc([?PUSH|Rest])->
	compile_number(push,Rest);
compile_opc([?DUP|Rest])->
	{ok,dup,Rest};
compile_opc([?COPY|Rest])->
	compile_number(copy,Rest);
compile_opc([?SWAP|Rest])->
	{ok,swap,Rest};
compile_opc([?POP|Rest])->
	{ok,pop,Rest};
compile_opc([?SLID|Rest])->
	compile_number(slide,Rest);

compile_opc([?ADD|Rest])->
	{ok,add,Rest};
compile_opc([?SUB|Rest])->
	{ok,sub,Rest};
compile_opc([?MUL|Rest])->
	{ok,mul,Rest};
compile_opc([?DIV|Rest])->
	{ok,'div',Rest};
compile_opc([?MOD|Rest])->
	{ok,mod,Rest};

compile_opc([?SET|Rest])->
	{ok,set,Rest};
compile_opc([?LOAD|Rest])->
	{ok,load,Rest};

compile_opc([?DEF|Rest])->
	compile_label(defun,Rest);
compile_opc([?CALL|Rest])->
	compile_label(call,Rest);
compile_opc([?JMP|Rest])->
	compile_label(jmp,Rest);
compile_opc([?JZ|Rest])->
	compile_label(jz,Rest);
compile_opc([?JNZ|Rest])->
	compile_label(jnz,Rest);
compile_opc([?RET|Rest])->
	{ok,ret,Rest};
compile_opc([?EXIT|Rest])->
	{ok,exit,Rest};
compile_opc([?EXIT2|Rest])->
	{ok,exit,Rest};

compile_opc([?IINT|Rest])->
	{ok,iint,Rest};
compile_opc([?OINT|Rest])->
	{ok,oint,Rest};
compile_opc([?ICHR|Rest])->
	{ok,ichr,Rest};
compile_opc([?OCHR|Rest])->
	{ok,ochr,Rest};

compile_opc(D)->
	{error,bad_opcode,D}.


compile_number(Op,[g|D])->
	compile_label(Op,D);
compile_number(Op,[m|D])->
	{Ok,N,Rest} = compile_binary(D,0),
	{Ok,{Op,-N},Rest};
compile_number(Op,[h|Rest])->
	{ok,{Op,0},Rest};
compile_number(Op,D)->
	{error,{Op,bad_number},D}.

compile_label(Op,D)->
	{Ok,N,Rest} = compile_binary(D,0),
	{Ok,{Op,N},Rest}.

compile_binary([g|Rest],Acc)->
	compile_binary(Rest,Acc bsl 1);
compile_binary([m|Rest],Acc)->
	compile_binary(Rest,(Acc bsl 1)+1);
compile_binary([h|Rest],Acc)->
	{ok,Acc,Rest};
compile_binary(D,_)->
	{error,bad_binary,D}.
