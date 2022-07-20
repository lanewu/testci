package py.netty.exception;

import org.apache.commons.lang.NotImplementedException;

import io.netty.buffer.ByteBuf;

public enum ExceptionType {
    GENERICEXCEPTION(0) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return GenericNettyException.fromBuffer(byteBuf);
        }
    },

    OVERLOADED(1) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return ServerOverLoadedException.fromBuffer(byteBuf);
        }
    },

    NOTSUPPORTED(2) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return NotSupportedException.fromBuffer(byteBuf);
        }
    },

    INVALIDPROTOCOL(3) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return InvalidProtocolException.fromBuffer(byteBuf);
        }
    },

    SERVERSHUTDOWN(4) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return ServerShutdownException.fromBuffer(byteBuf);
        }
    },

    SEGMENTNOTFOUND(5) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return SegmentNotFoundException.fromBuffer(byteBuf);
        }
    },

    NOTPRIMARY(6) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return NotPrimaryException.fromBuffer(byteBuf);
        }
    },


    SNAPSHOTMISMATCH(7) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return SnapshotMismatchException.fromBuffer(byteBuf);
        }
    },

    SEGMENTDELETE(8) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return SegmentDeleteException.fromBuffer(byteBuf);
        }
    },

    MEMBERSHIPLOWER(9) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return MembershipVersionLowerException.fromBuffer(byteBuf);
        }
    },

    NOTSECONDARY(10) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return NotSecondaryException.fromBuffer(byteBuf);
        }
    },

    SERVEREXCEPTION(11) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return ServerProcessException.fromBuffer(byteBuf);
        }
    },
    TOOBIGFRAME(12) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return TooBigFrameException.fromBuffer(byteBuf);
        }
    },
    MEMBERSHIPHIGER(13) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return MembershipVersionHigerException.fromBuffer(byteBuf);
        }
    },
    INCOMPLETE_GROUP(14) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return IncompleteGroupException.fromBuffer(byteBuf);
        }
    },
    NETWORK_UNHEALTHY(15) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return NetworkUnhealthyException.fromBuffer(byteBuf);
        }
    },
    NOTTEMPPRIMARY(16) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return NotTempPrimaryException.fromBuffer(byteBuf);
        }
    },
    NOTSECONDARYZOMBIE(17) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return NotSecondaryZombieException.fromBuffer(byteBuf);
        }
    },

    NETWORK_UNHEALTHY_BY_SD(18) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return NetworkUnhealthyBySDException.fromBuffer(byteBuf);
        }
    },

    HAS_NEW_PRIMARY(19) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return HasNewPrimaryException.fromBuffer(byteBuf);
        }
    },

    SERVER_PAUSED(20) {
        @Override
        public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
            return ServerPausedException.fromBuffer(byteBuf);
        }
    };

    private final int value;

    private ExceptionType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public AbstractNettyException fromBuffer(ByteBuf byteBuf) {
        return new GenericNettyException(new NotImplementedException("value: " + value));
    }
}
