/* header */
typedef struct CCL_VirtualMachine CCL_VirtualMachine;
typedef struct CCL_Instruction CCL_Instruction;
typedef struct CCL_InstructionTable CCL_InstructionTable;

struct CCL_InstructionTable {
  void (**f)(CCL_VirtualMachine*,CCL_Instruction);
};

struct CCL_VirtualMachine {
  int i;
};

void CCL_step(CCL_VirtualMachine *vm);


/* implementation */
void CCL_step(CCL_VirtualMachine *vm) {

}
