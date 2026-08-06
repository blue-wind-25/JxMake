/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
public class NestedSwitchRepro {
    void parse() {
    switch ((jj_ntk==-1)?jj_ntk_f():jj_ntk) {
    case THROWS:{
      jj_consume_token(THROWS);
      throwType = ReferenceTypeWithAnnotations();
throws_ = add(throws_, throwType);
      label_16:
      while (true) {
        switch ((jj_ntk==-1)?jj_ntk_f():jj_ntk) {
        case COMMA:{
          ;
          break;
          }
        default:
          jj_la1[39] = jj_gen;
          break label_16;
        }
        jj_consume_token(COMMA);
        throwType = ReferenceTypeWithAnnotations();
throws_ = add(throws_, throwType);
      }
      break;
      }
    default:
      jj_la1[40] = jj_gen;
      ;
    }
    }
}
