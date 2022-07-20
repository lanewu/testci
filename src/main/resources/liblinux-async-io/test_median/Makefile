CC=gcc --std=gnu99

ifndef JAVA_HOME
	@echo set defualt JAVA_HOME -> /usr/local/java/jdk1.8.0_65  
	JAVA_HOME := /usr/local/java/jdk1.8.0_65
endif

INCLUDES := -I ../include -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/linux

#SRCS := $(wildcard lib/*.c)
SRCS := test_median.c 
LIBS := -lpthread -lrt -llinux-async-io
TARGET := test-median.out
LIB_PATH := -L../

OBJS := $(SRCS:.c=.o)
DEPS := $(OBJS:.o=.d)

#FLAG= -g $(INCLUDES)
CFLAGS = -g
LDFLAGS := -DPY_PROFILING
#LDFLAGS :=

all:$(OBJS)	
	$(CC) $(CFLAGS) $(LDFLAGS) $(INCLUDES) -o $(TARGET) $(OBJS) $(LIB_PATH) $(LIBS)
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
