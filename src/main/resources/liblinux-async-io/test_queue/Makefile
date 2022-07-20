CC=gcc --std=gnu99
JAVA_HOME := /usr/lib/jvm/java-8-oracle/
INCLUDES := -I ../include -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/linux
#SRCS := $(wildcard lib/*.c)
SRCS := test_task_queue.c ../lib/task_queue.c
LIBS :=  -lpthread -lrt
TARGET := queue-test.out
LIB_PATH := 

OBJS := $(SRCS:.c=.o)
DEPS := $(OBJS:.o=.d)

#FLAG= -g $(INCLUDES)
CFLAGS = -g
LDFLAGS := -DPY_PROFILING
#LDFLAGS :=

all:$(OBJS)	
	$(CC) $(CFLAGS) $(LDFLAGS) $(INCLUDES) $(OBJS) -o $(TARGET) $(LIB_PATH) $(LIBS)
	@echo $(TARGET) has been built successfully!

-include $(DEPS) 

objs:$(OBJS)
	@echo Compile protocol objects finished.

%.o:%.c
	$(CC) $(CFLAGS) $(LDFLAGS) $(INCLUDES) -c $< -o $@
	
%.d:%.c
	@set -e; rm -f $@; \
	$(CC) $(INCLUDES) $< -MM -MT $(@:.d=.o) >$@
#$(CC) -MM $(INCLUDES) $< > $@.$$$$; \
#sed 's,\($*\)\.o[ :]*,\1.o $@ : ,g' < $@.$$$$ > $@; \
#rm -f $@.$$$$

install:
	cp inc/*.h ../../my_inc
	cp *.so ../../my_lib

clean:
	rm -f $(OBJS) $(DEPS)
	rm -f $(TARGET)
