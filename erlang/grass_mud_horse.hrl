-define(G,16#8349). %unicode
-define(M,16#6CE5).
-define(H,16#9A6C).
-define(R,16#6CB3).
-define(C,16#87F9).

-define(PUSH,g,g).
-define(DUP, g,h,g).
-define(COPY,g,m,g).
-define(SWAP,g,h,m).
-define(POP, g,h,h).
-define(SLID,g,m,h).

-define(ADD, m,g,g,g).
-define(SUB, m,g,g,m).
-define(MUL, m,g,g,h).
-define(DIV, m,g,m,g).
-define(MOD, m,g,m,m).

-define(SET, m,m,g).
-define(LOAD,m,m,m).

-define(DEF, h,g,g).
-define(CALL,h,g,m).
-define(JMP, h,g,h).
-define(JZ,  h,m,g).
-define(JN,  h,m,m).
-define(RET, h,m,h).
-define(EXIT,h,h,h).
-define(EXIT2,r,c).

-define(IINT,m,h,m,m).
-define(OINT,m,h,g,m).
-define(ICHR,m,h,m,g).
-define(OCHR,m,h,g,g).
